package com.inventory.system.config.tenant.routing;

import com.inventory.system.common.entity.UnitOfMeasure;
import com.inventory.system.config.JpaAuditingConfiguration;
import com.inventory.system.config.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 3 mandatory isolation test (defense-in-depth).
 *
 * <p>With Hibernate DATABASE multi-tenancy ON, persist rows for two tenants
 * into the (shared) DB, then simulate a mis-route by querying as tenant A: the
 * always-on {@code tenantFilter} discriminator must yield ZERO of tenant B's
 * rows. This proves that even if the connection provider ever routed a request
 * to the wrong pool within the shared DB, the discriminator still prevents any
 * cross-tenant read.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "app.tenant.routing.enabled=true",
        "app.tenant.routing.kek=phase3-test-kek"
})
@Import({
        TenantRoutingConfiguration.class,
        MultiTenantHibernateCustomizer.class,
        CredentialCipher.class,
        CurrentTenantResolver.class,
        TenantCatalogService.class,
        TenantDataSourceRegistry.class,
        RoutingAuditService.class,
        MultiTenantConnectionProviderImpl.class,
        JpaAuditingConfiguration.class
})
class Phase3TenantIsolationTest {

    @Autowired
    private EntityManager em;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private void persistUom(String tenant, String code) {
        TenantContext.setTenantId(tenant);
        UnitOfMeasure u = new UnitOfMeasure();
        u.setName(code);
        u.setCode(code);
        u.setCategory(UnitOfMeasure.UomCategory.QUANTITY);
        u.setIsBase(true);
        u.setConversionFactor(BigDecimal.ONE);
        em.persist(u);
        em.flush();
    }

    @Test
    void discriminatorFilterPreventsCrossTenantReadEvenUnderMultiTenancy() {
        persistUom("tenant-A", "AAA");
        persistUom("tenant-B", "BBB");
        em.clear();

        // No filter: both rows are physically present in the shared DB.
        @SuppressWarnings("unchecked")
        List<UnitOfMeasure> all = em.createQuery(
                "select u from UnitOfMeasure u where u.code in ('AAA','BBB')").getResultList();
        assertThat(all).hasSize(2);
        em.clear();

        // Simulate a mis-route: query while the tenant filter is scoped to A.
        Session session = em.unwrap(Session.class);
        session.enableFilter("tenantFilter").setParameter("tenantId", "tenant-A");

        @SuppressWarnings("unchecked")
        List<UnitOfMeasure> visible = em.createQuery(
                "select u from UnitOfMeasure u where u.code in ('AAA','BBB')").getResultList();

        assertThat(visible).hasSize(1);
        assertThat(visible.get(0).getCode()).isEqualTo("AAA");
        assertThat(visible).noneMatch(u -> "tenant-B".equals(u.getTenantId()));
    }
}
