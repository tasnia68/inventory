package com.inventory.system.config.tenant.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Production {@link TenantDataCopier}: app-level copy of one tenant's full
 * working set from the shared DB to its dedicated DB.
 *
 * <p>The shared schema mixes three table shapes; the copier handles each:
 * <ol>
 *   <li><b>Tenant-scoped</b> — table has a {@code tenant_id} column.
 *       Copy: {@code WHERE tenant_id::text = ?}.</li>
 *   <li><b>Join tables</b> — no {@code tenant_id} column but a FK referencing
 *       a tenant-scoped parent (e.g. {@code user_roles.user_id → users.id}).
 *       Copy: {@code WHERE <fk_col> IN (SELECT id FROM <parent> WHERE
 *       tenant_id::text = ?)}.</li>
 *   <li><b>Global lookup</b> — no {@code tenant_id} and no FK to any
 *       tenant-scoped table (e.g. {@code permissions}). Copy whole table.</li>
 * </ol>
 *
 * <p>Control-plane tables ({@code tenant_datasource}, {@code tenant_routing_audit},
 * {@code flyway_schema_history}) are never copied. The {@code tenants} table is
 * copied for the one matching row only, so the dedicated DB knows about itself
 * but never sees another tenant's row.
 *
 * <p>FK enforcement is bypassed for the bulk load (Hibernate's auto-generated
 * FKs are not DEFERRABLE). The copy runs in a single transaction so it is
 * atomic from the dedicated side.
 */
@Component
@ConditionalOnProperty(prefix = "app.tenant.routing", name = "enabled", havingValue = "true")
public class JdbcTenantDataCopier implements TenantDataCopier {

    private static final Logger log = LoggerFactory.getLogger(JdbcTenantDataCopier.class);

    private static final Set<String> CONTROL_PLANE_BLACKLIST = Set.of(
            "tenant_datasource", "tenant_routing_audit", "flyway_schema_history"
    );

    /**
     * Per-table copy plan. {@code filterSql} is a complete WHERE clause (may be
     * empty for whole-table copies); {@code parameterized} indicates whether
     * the filter binds the tenant-id parameter.
     */
    private record TablePlan(String name, String filterSql, boolean parameterized) {}

    private final JdbcTemplate sharedJdbc;

    public JdbcTenantDataCopier(DataSource sharedDataSource) {
        this.sharedJdbc = new JdbcTemplate(sharedDataSource);
    }

    @Override
    public Map<String, TableCount> copyTenant(String tenantId, String dedicatedUrl,
                                              String username, String password) {
        List<TablePlan> plans = buildCopyPlans();
        List<String> allTables = plans.stream().map(TablePlan::name).toList();

        Map<String, TableCount> report = new LinkedHashMap<>();
        try (Connection target = DriverManager.getConnection(dedicatedUrl, username, password)) {
            target.setAutoCommit(false);

            boolean replicaMode = trySetReplicaMode(target);
            if (!replicaMode) {
                togglePerTableTriggers(target, allTables, false);
            }
            try (PreparedStatement defer = target.prepareStatement("SET CONSTRAINTS ALL DEFERRED")) {
                defer.execute();
            }

            // Idempotency: a previously-failed cutover may have left rows
            // around. TRUNCATE everything we are about to repopulate; safe
            // because the dedicated DB only ever holds this one tenant.
            for (String t : allTables) {
                try (PreparedStatement trunc = target.prepareStatement(
                        "TRUNCATE TABLE " + quote(t) + " CASCADE")) {
                    trunc.execute();
                }
            }

            for (TablePlan plan : plans) {
                long src = countSource(plan, tenantId);
                long copied = copyTable(plan, tenantId, target);
                report.put(plan.name(), new TableCount(src, copied));
            }

            if (replicaMode) {
                try (PreparedStatement reset = target.prepareStatement(
                        "SET session_replication_role = DEFAULT")) {
                    reset.execute();
                }
            } else {
                togglePerTableTriggers(target, allTables, true);
            }
            target.commit();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new TenantDatasourceUnavailableException(
                    "Data copy to dedicated DB failed for tenant " + tenantId + ": " + e.getMessage(), e);
        }
        log.info("Copied {} tables for tenant {} to dedicated DB", report.size(), tenantId);
        return report;
    }

    /**
     * Inspect the shared schema and build a per-table copy plan. Tenant-scoped
     * tables come first, then join tables (whose FK filter depends on data
     * that just landed), then global lookups.
     */
    private List<TablePlan> buildCopyPlans() {
        Set<String> tenantScoped = new HashSet<>(sharedJdbc.queryForList(
                "SELECT table_name FROM information_schema.columns "
                + "WHERE column_name = 'tenant_id' AND table_schema = 'public' "
                + "ORDER BY table_name", String.class));

        List<String> allTables = sharedJdbc.queryForList(
                "SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' "
                + "ORDER BY table_name", String.class);

        List<TablePlan> plans = new ArrayList<>();

        // 1) Tenant-scoped tables — direct tenant_id filter.
        for (String t : tenantScoped) {
            plans.add(new TablePlan(t, "WHERE tenant_id::text = ?", true));
        }

        // 2/3) Everything else — choose join filter or whole-table.
        for (String t : allTables) {
            if (tenantScoped.contains(t)) continue;
            if (CONTROL_PLANE_BLACKLIST.contains(t)) continue;

            // Special case: the `tenants` row matching this tenant's id is
            // copied; other tenants' rows must NEVER land in this DB.
            if ("tenants".equals(t)) {
                plans.add(new TablePlan(t, "WHERE id::text = ?", true));
                continue;
            }

            String joinFilter = findJoinFilterToTenantScopedParent(t, tenantScoped);
            if (joinFilter != null) {
                plans.add(new TablePlan(t, joinFilter, true));
            } else {
                // Global lookup — same for every tenant.
                plans.add(new TablePlan(t, "", false));
            }
        }
        return plans;
    }

    /**
     * Look for a FK from {@code table} to any tenant-scoped parent. If found,
     * returns a WHERE clause that filters via the parent's tenant_id.
     */
    private String findJoinFilterToTenantScopedParent(String table, Set<String> tenantScoped) {
        List<Map<String, Object>> fks = sharedJdbc.queryForList(
                "SELECT kcu.column_name AS col, ccu.table_name AS ref_table, ccu.column_name AS ref_col "
                + "FROM information_schema.table_constraints tc "
                + "JOIN information_schema.key_column_usage kcu "
                + "  ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema "
                + "JOIN information_schema.constraint_column_usage ccu "
                + "  ON ccu.constraint_name = tc.constraint_name AND ccu.table_schema = tc.table_schema "
                + "WHERE tc.constraint_type = 'FOREIGN KEY' "
                + "  AND tc.table_schema = 'public' AND tc.table_name = ?",
                table);
        for (Map<String, Object> fk : fks) {
            String refTable = String.valueOf(fk.get("ref_table"));
            if (tenantScoped.contains(refTable)) {
                String col = String.valueOf(fk.get("col"));
                String refCol = String.valueOf(fk.get("ref_col"));
                return "WHERE " + quote(col) + " IN (SELECT " + quote(refCol)
                        + " FROM " + quote(refTable) + " WHERE tenant_id::text = ?)";
            }
        }
        return null;
    }

    private long countSource(TablePlan plan, String tenantId) {
        String sql = "SELECT COUNT(*) FROM " + quote(plan.name())
                + (plan.filterSql().isEmpty() ? "" : " " + plan.filterSql());
        Long n = plan.parameterized()
                ? sharedJdbc.queryForObject(sql, Long.class, tenantId)
                : sharedJdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }

    private long copyTable(TablePlan plan, String tenantId, Connection target) throws Exception {
        String sql = "SELECT * FROM " + quote(plan.name())
                + (plan.filterSql().isEmpty() ? "" : " " + plan.filterSql());
        long copied = 0;
        try (Connection src = sharedJdbc.getDataSource().getConnection();
             PreparedStatement sel = src.prepareStatement(
                     sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            if (plan.parameterized()) sel.setString(1, tenantId);
            sel.setFetchSize(500);
            try (ResultSet rs = sel.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                List<String> colNames = new ArrayList<>();
                for (int i = 1; i <= cols; i++) colNames.add(md.getColumnName(i));

                String insert = "INSERT INTO " + quote(plan.name()) + " ("
                        + String.join(",", colNames.stream().map(this::quote).toList())
                        + ") VALUES (" + "?,".repeat(cols - 1) + "?)";

                try (PreparedStatement ins = target.prepareStatement(insert)) {
                    while (rs.next()) {
                        for (int i = 1; i <= cols; i++) ins.setObject(i, rs.getObject(i));
                        ins.addBatch();
                        copied++;
                        if (copied % 500 == 0) ins.executeBatch();
                    }
                    ins.executeBatch();
                }
            }
        }
        return copied;
    }

    private boolean trySetReplicaMode(Connection c) {
        try (PreparedStatement ps = c.prepareStatement("SET session_replication_role = 'replica'")) {
            ps.execute();
            return true;
        } catch (Exception e) {
            log.info("session_replication_role=replica unavailable ({}); falling back to per-table DISABLE TRIGGER ALL", e.getMessage());
            return false;
        }
    }

    private void togglePerTableTriggers(Connection c, List<String> tables, boolean enable) throws java.sql.SQLException {
        String verb = enable ? "ENABLE" : "DISABLE";
        for (String table : tables) {
            try (PreparedStatement ps = c.prepareStatement(
                    "ALTER TABLE " + quote(table) + " " + verb + " TRIGGER ALL")) {
                ps.execute();
            }
        }
    }

    /** Defensive identifier quoting; identifiers come from information_schema, not user input. */
    private String quote(String identifier) {
        return "\"" + identifier.replace("\"", "") + "\"";
    }
}
