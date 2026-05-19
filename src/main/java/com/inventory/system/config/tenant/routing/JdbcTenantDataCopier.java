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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Production {@link TenantDataCopier}: app-level, table-by-table copy of one
 * tenant's rows from the shared DB to its dedicated DB.
 *
 * <p>Tenant-scoped tables are discovered from {@code information_schema}
 * (every table carrying a {@code tenant_id} column). On the dedicated side the
 * copy runs in a single transaction with {@code SET CONSTRAINTS ALL DEFERRED}
 * so foreign-key ordering does not matter. Each table reports source vs copied
 * counts; {@link TenantCutoverService} refuses to flip unless all match.
 *
 * <p>The dedicated connection is opened directly (not pooled) for the duration
 * of the one-shot cutover.
 */
@Component
@ConditionalOnProperty(prefix = "app.tenant.routing", name = "enabled", havingValue = "true")
public class JdbcTenantDataCopier implements TenantDataCopier {

    private static final Logger log = LoggerFactory.getLogger(JdbcTenantDataCopier.class);

    private final JdbcTemplate sharedJdbc;

    public JdbcTenantDataCopier(DataSource sharedDataSource) {
        this.sharedJdbc = new JdbcTemplate(sharedDataSource);
    }

    @Override
    public Map<String, TableCount> copyTenant(String tenantId, String dedicatedUrl,
                                              String username, String password) {
        List<String> tables = sharedJdbc.queryForList(
                "SELECT table_name FROM information_schema.columns "
                + "WHERE column_name = 'tenant_id' AND table_schema = 'public' "
                + "ORDER BY table_name", String.class);

        Map<String, TableCount> report = new LinkedHashMap<>();
        try (Connection target = DriverManager.getConnection(dedicatedUrl, username, password)) {
            target.setAutoCommit(false);
            try (PreparedStatement defer = target.prepareStatement("SET CONSTRAINTS ALL DEFERRED")) {
                defer.execute();
            }
            for (String table : tables) {
                long src = sharedCount(table, tenantId);
                long copied = copyTable(table, tenantId, target);
                report.put(table, new TableCount(src, copied));
            }
            target.commit();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new TenantDatasourceUnavailableException(
                    "Data copy to dedicated DB failed for tenant " + tenantId + ": " + e.getMessage(), e);
        }
        log.info("Copied {} tenant-scoped tables for {} to dedicated DB", report.size(), tenantId);
        return report;
    }

    private long sharedCount(String table, String tenantId) {
        Long n = sharedJdbc.queryForObject(
                "SELECT COUNT(*) FROM " + quote(table) + " WHERE tenant_id = ?",
                Long.class, tenantId);
        return n == null ? 0 : n;
    }

    private long copyTable(String table, String tenantId, Connection target) throws Exception {
        long copied = 0;
        try (PreparedStatement sel = sharedSelect(table, tenantId);
             ResultSet rs = sel.executeQuery()) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            List<String> colNames = new ArrayList<>();
            for (int i = 1; i <= cols; i++) colNames.add(md.getColumnName(i));

            String insert = "INSERT INTO " + quote(table) + " ("
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
        return copied;
    }

    private PreparedStatement sharedSelect(String table, String tenantId) throws Exception {
        Connection c = sharedJdbc.getDataSource().getConnection();
        PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM " + quote(table) + " WHERE tenant_id = ?",
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        ps.setString(1, tenantId);
        ps.setFetchSize(500);
        return ps;
    }

    /** Defensive identifier quoting; identifiers come from information_schema, not user input. */
    private String quote(String identifier) {
        return "\"" + identifier.replace("\"", "") + "\"";
    }
}
