# Per-Tenant Dedicated Database — Operations Runbook

Hybrid ("Bridge") multi-tenancy: most tenants share one Postgres (tenant_id
`@Filter` discriminator); selected tenants get their own Postgres instance.
Everything is gated by `app.tenant.routing.enabled` (default **false** = the
system behaves exactly as before).

## 1. Enabling the feature (staged rollout)

1. Set the KEK in the environment (never in the DB or git):
   `MASTERINVENTORY_TENANT_KEK=<32+ char strong secret>`
   (optionally `MASTERINVENTORY_TENANT_KEK_ID=env:v1`).
2. Enable in staging first: `APP_TENANT_ROUTING_ENABLED=true`. With **no**
   `tenant_datasource` rows every tenant still resolves to the shared pool —
   Hibernate runs in DATABASE multi-tenancy but behaviour is a no-op.
3. Pilot one internal tenant through the cutover flow (section 3), verify,
   then enable in production.

Disable instantly by setting the flag back to `false` (routing beans vanish;
Hibernate reverts to the plain shared EMF).

## 2. Moving a tenant to a dedicated database

Super-Admin → Tenants → select tenant → **Database / Tenant profile**:

1. Provision a dedicated Postgres instance; create a least-privilege role
   scoped to that database only.
2. Mode = **Dedicated**, enter JDBC URL / username / password → **Save**
   (credentials are encrypted AES-256-GCM; never returned or logged).
3. **Test connection**.
4. **Run migrations** — Flyway brings the dedicated DB to the app baseline;
   refuses if the URL equals the shared DB URL or mode ≠ DEDICATED.
5. **Provision & cut over** — sets status `MIGRATING` (tenant fails closed for
   the window, seconds), copies the tenant's rows, verifies per-table counts,
   then an atomic single-row flip to `ACTIVE`. Any failure → `PENDING` +
   `last_error`; **shared data is untouched**, so the tenant stays on shared
   until retried.

## 3. Fail-closed behaviour (by design)

A DEDICATED tenant whose DB is unreachable / credentials invalid / status not
`ACTIVE` / schema behind baseline returns **HTTP 503** — it is NEVER served
from the shared DB. Every DEDICATED/DENIED decision is recorded in
`tenant_routing_audit`. Defence-in-depth: the `tenantFilter` discriminator is
always enabled, so even a mis-route cannot read another tenant's rows.

## 4. Routine operations

- **Force-disable a tenant's dedicated DB**: set `tenant_datasource.status`
  to `DISABLED` (fails closed) or revert mode to `SHARED` via the UI (clears
  credentials).
- **Rotate a tenant's DB credentials**: update creds via the UI PUT; the
  catalog cache is invalidated and the pool is rebuilt on next use. To force
  immediate pool drop, toggle status (DISABLED→ACTIVE) which calls
  `registry.evict`.
- **Rotate the KEK**: decrypt-then-reencrypt all `*_enc` columns with the new
  key (offline maintenance script), bump `MASTERINVENTORY_TENANT_KEK_ID`,
  deploy the new `MASTERINVENTORY_TENANT_KEK`. Old ciphertext is unreadable
  once the old KEK is removed — do this in a maintenance window.
- **Stale-pool eviction** is automatic: bounded LRU
  (`app.tenant.routing.pool-cache-max-size`, default 200) closes idle pools.

## 5. Connection budget (multi-node)

Each app node keeps its own LRU of dedicated pools. Total connections ≈
`nodes × pool-cache-max-size × pool-max-size`. Size `pool-max-size` (default
5) and the cache cap per the dedicated Postgres instances' limits. Decision on
file: accept `nodes × cap` (no LB pinning).

## 6. Known limitations / required follow-ups

- **Tenant-agnostic schedulers** (`ReportSchedulingServiceImpl`,
  `StockReservationServiceImpl.cleanupExpiredReservations`) run without a
  `TenantContext`, so they operate only on the **shared** pool. DEDICATED
  tenants' scheduled reports / reservation cleanup will **not** run until
  these jobs are refactored to iterate tenants and execute each inside
  `TenantContext.callWithTenant(...)` (covering DEDICATED routing). This is a
  functional gap, **not** a data leak. `SteadfastAutomationScheduler` already
  iterates tenants and clears context per iteration — use it as the pattern.
- **Metrics**: routing currently observable via the `tenant_routing_audit`
  table only. Micrometer counters
  (`tenant.routing.dedicated|denied`, `tenant.pool.created|evicted`) are
  deferred because they require adding the actuator/Micrometer dependency —
  add deliberately, then wire counters in `MultiTenantConnectionProviderImpl`
  and `TenantDataSourceRegistry`.
- **Schema-baseline gate** ships disabled (`app.tenant.routing
  .expected-schema-version` blank). After provisioning stamps
  `flyway_version`, set this to the current baseline (e.g. `59`) so a
  dedicated DB that drifts behind app migrations fails closed.
- **Backups/DR are now per dedicated instance** — extend backup tooling and
  monitoring to every dedicated Postgres.
- **Async `@Async` paths**: `TenantAwareTaskDecorator` already propagates
  context to configured executors; any new executor must use it or be
  blocked from tenant data.

## 7. Rollback

- Per tenant: set mode `SHARED` (UI) — the catalog flips, shared data was
  never removed (grace-purge is manual/deferred), so the tenant is served
  from shared again immediately.
- Whole feature: `APP_TENANT_ROUTING_ENABLED=false` and redeploy.
