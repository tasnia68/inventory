# Tenant Management and Tenant-Aware Logging

This backend uses a shared-database, shared-schema multi-tenant model. Tenant-owned rows carry a `tenant_id` column, request processing establishes one tenant context per request thread, and Hibernate filters plus tenant-qualified repository methods keep normal business queries inside that tenant boundary.

## Runtime Tenant Flow

1. Incoming API requests enter `TenantContextFilter` before the application controllers.
   Code: `src/main/java/com/inventory/system/config/tenant/TenantContextFilter.java`

2. The filter resolves a tenant from one of these sources:
   - `X-Tenant-ID` request header for backoffice/API calls.
   - JWT tenant claim from the `Authorization: Bearer ...` token.
   - Storefront host/domain for `/api/v1/storefront/public/**` requests.
   - Webhook URL segment for `/api/webhooks/{provider}/{tenantId}/...` requests.

3. If a protected API request has no tenant, the request fails closed with `400 Missing tenant context`.

4. Once resolved, `TenantContextFilter` calls `TenantContext.setTenantId(tenantId)` and enables the Hibernate filter named `tenantFilter` with the same tenant value.

5. The request continues through security, services, repositories, and controllers with the current tenant stored in a `ThreadLocal`.

6. The filter clears the tenant in a `finally` block with `TenantContext.clear()` so servlet worker threads cannot leak one tenant into the next request.

## Tenant Context

`TenantContext` is the in-process holder for the current tenant.

Code: `src/main/java/com/inventory/system/config/tenant/TenantContext.java`

Important behavior:

- `setTenantId(...)` rejects blank tenant IDs and trims valid values.
- `getCurrentTenantId()` returns the nullable current tenant.
- `requireTenantId()` fails fast when code tries to create or access tenant-scoped data without a tenant.
- `runWithTenant(...)` and `callWithTenant(...)` temporarily switch tenant context and restore the previous tenant afterward.
- `clear()` removes the thread-local tenant value.

This is important because servlet containers reuse threads. Without `clear()`, tenant A could accidentally remain on a thread that later handles tenant B.

## Database Isolation

Most tenant-owned entities inherit from `BaseEntity`.

Code: `src/main/java/com/inventory/system/common/entity/BaseEntity.java`

`BaseEntity` provides:

- A non-null, immutable `tenant_id` column.
- Hibernate filter definition: `tenantFilter`.
- Filter condition: `tenant_id = :tenantId`.
- `@PrePersist` tenant assignment using `TenantContext.requireTenantId()`.

That means new tenant-owned records cannot be persisted without an active tenant context, and normal Hibernate entity queries are constrained by the enabled tenant filter.

## Transaction Boundary Protection

`TenantFilterAspect` re-enables the Hibernate tenant filter at transactional method entry.

Code: `src/main/java/com/inventory/system/config/tenant/TenantFilterAspect.java`

This exists because the servlet filter may enable the Hibernate filter on one `Session`, while Spring's transactional service layer can use another session. The aspect closes that gap by enabling `tenantFilter` again on the session actually used by `@Transactional` methods.

## Async Tenant Propagation

`TenantContext` is backed by `ThreadLocal`, so it does not automatically cross thread boundaries. `TenantAwareTaskDecorator` captures the tenant from the submitting thread and restores it around async work.

Code: `src/main/java/com/inventory/system/config/tenant/TenantAwareTaskDecorator.java`

Reusable wrapper helpers also exist directly on `TenantContext` for code that needs to submit work to another executor explicitly.

Code: `src/main/java/com/inventory/system/config/tenant/TenantContext.java`

It deliberately does not invent a default tenant when none exists. That is safer for production multi-tenancy because missing tenant context should be visible instead of silently using the wrong tenant.

### What Is Covered Today

- The servlet request thread established by `TenantContextFilter`.
- Background work submitted to `reportingTaskExecutor`.
- Spring MVC async request processing that uses the configured MVC async executor.
- `CompletableFuture` and `ForkJoinPool` code that routes through `TenantAsync`.

Code references:

- `src/main/java/com/inventory/system/config/AsyncExecutionConfiguration.java`
- `src/main/java/com/inventory/system/config/WebMvcConfiguration.java`

`WebMvcConfiguration` now sets a tenant-aware `mvcAsyncTaskExecutor` through `configureAsyncSupport(...)`, so controller methods that rely on Spring MVC async execution do not silently lose tenant context when work resumes on an executor-managed worker thread.

For unmanaged async primitives, the backend now provides `TenantAsync`.

Code:

- `src/main/java/com/inventory/system/config/tenant/TenantAsync.java`
- `src/test/java/com/inventory/system/config/tenant/TenantAsyncTest.java`

`TenantAsync` provides approved tenant-aware entry points for:

- `CompletableFuture.runAsync(...)`
- `CompletableFuture.supplyAsync(...)`
- `ForkJoinPool.commonPool()` submission
- explicit `ForkJoinPool` submission
- wrapping a general `Executor` with `TenantAsync.tenantAware(...)`

Recommended examples:

```java
TenantAsync.runAsync(() -> notificationService.publishEvent());

TenantAsync.supplyAsync(() -> reportService.buildSnapshot());

TenantAsync.supplyAsync(
   () -> reportService.buildSnapshot(),
   reportingTaskExecutor
);

TenantAsync.submit(() -> reconciliationService.runBatch());
```

If code must pass an executor into another API, use the tenant-aware wrapper or the Spring bean named `tenantAwareCommonPoolExecutor`.

### What Is Still Unsafe Unless You Propagate Explicitly

- direct raw `ForkJoinPool.commonPool()` calls that bypass `TenantAsync`
- direct `CompletableFuture.runAsync(...)` or `supplyAsync(...)` calls that bypass `TenantAsync` and do not pass a tenant-aware executor
- raw `new Thread(...)`
- third-party libraries that hop onto their own unmanaged executors

For those cases, capture and reapply tenant context explicitly with `TenantContext.wrap(...)`, `TenantContext.wrapSupplier(...)`, or `TenantContext.wrapCallable(...)`, or route the work through `TenantAsync`.

Example:

```java
CompletableFuture.supplyAsync(
   TenantContext.wrapSupplier(() -> reportService.buildSnapshot()),
   mvcAsyncTaskExecutor
);
```

Or with an unmanaged pool when you cannot avoid it:

```java
ForkJoinPool.commonPool().submit(
   TenantContext.wrap(() -> reconciliationService.runBatch())
);
```

The design remains fail-closed: if no tenant existed on the submitting thread, the wrapped task also runs without a synthetic tenant.

## Authentication and Tenant Matching

`JwtAuthenticationFilter` validates tenant consistency between the JWT and current request context.

Code: `src/main/java/com/inventory/system/security/JwtAuthenticationFilter.java`

If the token tenant and request tenant are both present but different, the backend returns `403 Tenant mismatch in token`. This prevents a user from authenticating with a token from tenant A while sending tenant B in a header.

User loading is tenant-qualified through `CustomUserDetailsService`, and active service-level user lookups use tenant-qualified repository methods where tenant identity matters.

Code references:

- `src/main/java/com/inventory/system/security/CustomUserDetailsService.java`
- `src/main/java/com/inventory/system/service/UserServiceImpl.java`
- `src/main/java/com/inventory/system/service/impl/PosServiceImpl.java`
- `src/main/java/com/inventory/system/service/PayrollServiceImpl.java`

## Request Isolation Tests

Tenant isolation is covered at two levels:

- `TenantContextTest` verifies thread-local lifecycle, required-tenant failures, scoped tenant switching, and concurrent thread isolation.
- `TenantContextTest` also verifies explicit wrapper-based propagation for cross-thread runnable and callable execution.
- `TenantAsyncTest` verifies `CompletableFuture` and `ForkJoinPool` propagation and cleanup behavior with scenario-specific test cases.
- `TenantAwareTaskDecoratorTest` verifies async tenant propagation and confirms no synthetic default tenant is introduced.
- `TenantContextFilterConcurrencyTest` drives two concurrent real filter requests with different `X-Tenant-ID` values and verifies each request sees only its own tenant and clears tenant context after completion.

Code references:

- `src/test/java/com/inventory/system/config/tenant/TenantContextTest.java`
- `src/test/java/com/inventory/system/config/tenant/TenantAwareTaskDecoratorTest.java`
- `src/test/java/com/inventory/system/config/tenant/TenantContextFilterConcurrencyTest.java`

Focused validation command:

```bash
cd inventory
export JAVA_HOME=/Users/sanzar/Library/Java/JavaVirtualMachines/corretto-17.0.14/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw -Dtest=TenantContextTest,TenantAwareTaskDecoratorTest,TenantContextFilterConcurrencyTest test
```

## Tenant-Aware Log Format

The log prefix comes from Spring Boot logging configuration.

Code: `src/main/resources/application.yml`

```yaml
logging:
  pattern:
    level: "%5p [tenant:%X{tenant_id:-na}] [request:%X{request_id:-na}] [user:%X{user_name:-anonymous}]"
```

This pattern uses SLF4J MDC values:

- `%X{tenant_id:-na}` prints the MDC value named `tenant_id`; if missing, it prints `na`.
- `%X{request_id:-na}` prints the MDC value named `request_id`; if missing, it prints `na`.
- `%X{user_name:-anonymous}` prints the MDC value named `user_name`; if missing, it prints `anonymous`.

That is why startup logs look like this:

```text
DEBUG [tenant:na] [request:na] [user:anonymous]
```

During startup there is no HTTP request, no tenant context, and no authenticated user, so all MDC fallbacks are used.

During a real request, `RequestCorrelationFilter` sets MDC values.

Code: `src/main/java/com/inventory/system/config/filter/RequestCorrelationFilter.java`

It does the following:

- Reads `X-Request-ID`; if missing, generates a UUID.
- Adds the request ID to MDC as `request_id`.
- Copies `TenantContext.getCurrentTenantId()` into MDC as `tenant_id` when present.
- Copies the authenticated principal name into MDC as `user_name` when present.
- Removes all three MDC values in `finally`.

Filter order is configured in `WebConfiguration`.

Code: `src/main/java/com/inventory/system/config/WebConfiguration.java`

- `TenantContextFilter` is registered for `/api/*` with order `-101`.
- Spring Security then authenticates the request.
- `RequestCorrelationFilter` is registered for `/*` with order `2`, so it can read the tenant context and, for authenticated requests, the security principal.

Example request log after tenant and request correlation are set:

```text
INFO [tenant:platform] [request:5d7f0b72-7f2b-4c3b-9d2a-3be5a4ad71bb] [user:superadmin@test.com]
```

There is also an older `MdcLoggingFilter` class that can put only `tenant_id` into MDC, but it is not registered in `WebConfiguration`. The active request correlation mechanism is `RequestCorrelationFilter`.

## Why This Is Industry Standard

The implementation follows common production multi-tenancy patterns for a shared-schema SaaS backend:

- Request-scoped tenant resolution at the edge of the application.
- Fail-closed behavior when tenant context is missing.
- Thread-local tenant context with guaranteed cleanup.
- Database-level tenant discriminator column on tenant-owned entities.
- Hibernate filter enforcement for normal ORM queries.
- Transaction-boundary reinforcement to avoid session mismatch gaps.
- Tenant-qualified repository methods for sensitive direct lookups such as users and invitations.
- JWT tenant mismatch protection.
- Tenant-aware async propagation.
- Tenant/request/user correlation in logs using MDC.
- Concurrency tests proving tenant context does not leak between requests.

## Remaining Hardening Opportunities

The current implementation is strong, but these are the next steps for stricter enterprise-grade tenancy:

- Continue replacing `findById` and `findAll` service paths with explicit `...AndTenantId` repository methods for the most sensitive aggregates.
- Add integration tests that hit real HTTP endpoints with two tenants and verify database results, not only filter/thread context.
- Consider database row-level security for an additional defense-in-depth layer if PostgreSQL operational complexity is acceptable.
- Remove or register/deprecate the unused `MdcLoggingFilter` to avoid confusion.
- Lower dev logging levels from `DEBUG`/`TRACE` to `INFO` when you want cleaner local logs.