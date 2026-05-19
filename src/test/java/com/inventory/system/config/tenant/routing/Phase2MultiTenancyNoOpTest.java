package com.inventory.system.config.tenant.routing;

import com.inventory.system.common.entity.UnitOfMeasure;
import com.inventory.system.config.JpaAuditingConfiguration;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.repository.UnitOfMeasureRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 2 proof: with {@code app.tenant.routing.enabled=true} the application
 * runs Hibernate in {@code DATABASE} multi-tenancy, yet — because no tenant is
 * DEDICATED — every tenant still resolves to the shared pool and persistence
 * behaves exactly as before (a functional no-op).
 *
 * <p>Uses the {@code @DataJpaTest} JPA slice on purpose: it does not load the
 * web context (so the unrelated, pre-existing {@code MinioConfig} failure does
 * not apply) while still building the real Hibernate EMF through Spring Boot
 * autoconfig + our {@link MultiTenantHibernateCustomizer}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "app.tenant.routing.enabled=true",
        "app.tenant.routing.kek=phase2-test-kek"
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
class Phase2MultiTenancyNoOpTest {

    @Autowired
    private UnitOfMeasureRepository uomRepository;

    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private MultiTenantConnectionProviderImpl connectionProvider;

    @Autowired
    private CurrentTenantResolver resolver;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void hibernateMultiTenancyIsActuallyEnabled() {
        SessionFactoryImplementor sf = emf.unwrap(SessionFactoryImplementor.class);
        assertThat(sf.getSessionFactoryOptions().isMultiTenancyEnabled())
                .as("DATABASE multi-tenancy must be active when the flag is on")
                .isTrue();
        // and our beans are the wired ones
        assertThat(connectionProvider).isNotNull();
        assertThat(resolver).isNotNull();
    }

    @Test
    void entityRoundTripsRoutedToSharedPool() {
        TenantContext.setTenantId("tenant-A");

        UnitOfMeasure uom = new UnitOfMeasure();
        uom.setName("Kilogram");
        uom.setCode("KG-" + UUID.randomUUID());
        uom.setCategory(UnitOfMeasure.UomCategory.WEIGHT);
        uom.setIsBase(true);
        uom.setConversionFactor(BigDecimal.ONE);

        UnitOfMeasure saved = uomRepository.saveAndFlush(uom);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTenantId()).isEqualTo("tenant-A"); // prePersist stamp intact

        UnitOfMeasure reloaded = uomRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Kilogram");
        assertThat(reloaded.getTenantId()).isEqualTo("tenant-A");
    }

    @Test
    void resolverReturnsTenantThenSharedSentinel() {
        TenantContext.setTenantId("tenant-B");
        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo("tenant-B");
        TenantContext.clear();
        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo("__shared__");
    }
}
