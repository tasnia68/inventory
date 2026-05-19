# Per-Tenant Dedicated Database — What We Built & Why

## The problem

MASTERINVENTORY was a **single shared database** for every tenant. Isolation
was logical only: one Postgres, a `tenant_id` column on every row, and a
Hibernate `@Filter` (`tenant_id = :tenantId`) scoping queries to the current
tenant.

That model has hard ceilings as the platform grows:

- **No physical isolation / blast radius.** One bad migration, a runaway
  query, data corruption, or a noisy "whale" tenant degrades or risks
  *every* tenant at once. A single discriminator bug = cross-tenant exposure.
- **Compliance / enterprise demands.** Larger or regulated customers require
  their data in a separate database (sometimes separate infrastructure) —
  impossible with one shared DB.
- **Scaling limits.** A single Postgres is one write ceiling, one backup, one
  failure domain for the whole customer base.
- **All-or-nothing.** Moving everyone to database-per-tenant would be a
  massive, risky rewrite and is wasteful for the long tail of small tenants.

## What we implemented

A **hybrid ("Bridge") multi-tenancy** model: the shared database stays the
default for most tenants, and **specific tenants can be moved to their own
dedicated Postgres instance**, configured and switched over at runtime — with
**zero cross-tenant data leakage** and **fail-closed** behaviour.

Built in 6 gated, individually-reverted phases (feature flag
`app.tenant.routing.enabled`, default **off** → behaviour identical to before):

1. **Catalog + crypto.** A control-plane `tenant_datasource` table (source of
   truth) and append-only `tenant_routing_audit`. DB credentials encrypted
   AES-256-GCM with a key-encryption key from the environment (never stored).
2. **Routing infrastructure.** A tenant identifier resolver, a catalog
   service (short-TTL cache, fail-closed), a **bounded-LRU pool registry**
   (lazily creates/validates/evicts per-tenant connection pools so total
   connections stay bounded), and a Hibernate `MultiTenantConnectionProvider`.
3. **Hibernate `DATABASE` multi-tenancy**, wired without ripping out Spring
   Boot's JPA autoconfig. (This phase caught and fixed a real circular
   dependency — the control-plane now reads via plain JDBC so it can't depend
   on the persistence unit it configures.)
4. **Hardening.** Schema-baseline gate (a dedicated DB behind the app's
   migrations fails closed), fail-closed → HTTP 503, plus the mandatory
   isolation and fail-closed test suites.
5. **Provisioning.** Programmatic Flyway migration of a dedicated DB to the
   app baseline, with guards that refuse to ever run tenant migrations
   against the shared DB.
6. **Cutover + admin surface.** A shared→dedicated migration: hold the tenant
   → migrate its schema → copy only that tenant's rows → verify per-table
   row counts → **atomic flip** → invalidate caches. A super-admin API + a
   "Database / Tenant profile" UI panel (write-only credentials, never shown)
   to configure, test, migrate, and cut over a tenant. Plus an operations
   runbook.

## How it solves the problem

| Problem | Solution |
|---|---|
| Blast radius / noisy neighbour | A flagged tenant runs on its own Postgres; its load, failures, and migrations are isolated from everyone else. |
| Compliance / enterprise isolation | True physical separation per tenant (separate instance), opt-in per tenant. |
| Scaling ceiling | Heavy tenants get dedicated capacity; the shared DB carries only the long tail. |
| Avoid a risky big-bang rewrite | Shared schema stays the default and unchanged; dedicated is **additive and opt-in**, the existing model is reused inside each database. |
| Data leakage risk | Routing only from server-validated tenant context; the `tenant_id` discriminator filter stays **always on** (defence-in-depth — a mis-route still returns zero foreign rows, proven by test); credentials encrypted and write-only. |
| Operational safety | **Fail-closed**: a dedicated DB that is down / misconfigured / schema-behind returns 503 — it never silently falls back to the shared DB. Cutover verifies row counts and only flips atomically; any failure leaves the tenant safely on shared. |

## Safety properties (proven by tests)

- **No production impact while disabled** — every routing component is
  conditional on the flag; with it off the application is byte-identical to
  before (full test suite unchanged across all 6 phases).
- **Isolation** — under multi-tenancy, with the discriminator scoped to
  tenant A, tenant B's rows are invisible even though both physically exist.
- **Fail-closed** — a dedicated tenant whose DB is unreachable throws, audits
  a `DENIED` row, and never returns or writes the shared connection; other
  tenants keep working.
- **Cutover correctness** — mismatched copy counts abort the flip and roll
  back to a non-active (fail-closed) state with the shared data untouched.

## Status

Implemented, tested (39 dedicated routing tests, zero regressions vs.
baseline), documented, committed, and pushed. **Off by default** — see
[TENANT_DEDICATED_DB_RUNBOOK.md](TENANT_DEDICATED_DB_RUNBOOK.md) to enable and
operate it, including the documented follow-ups (tenant-agnostic schedulers,
deferred metrics, schema-baseline gate activation).
