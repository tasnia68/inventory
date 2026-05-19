package com.inventory.system.config.tenant.routing;

import com.inventory.system.common.entity.TenantDatasource;
import com.inventory.system.common.entity.TenantDatasourceMode;
import com.inventory.system.common.entity.TenantDatasourceStatus;
import com.inventory.system.repository.TenantDatasourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantCatalogServiceTest {

    private TenantDatasourceRepository repo;
    private CredentialCipher cipher;
    private TenantRoutingProperties props;
    private TenantCatalogService service;

    @BeforeEach
    void setUp() {
        repo = mock(TenantDatasourceRepository.class);
        props = new TenantRoutingProperties();
        props.setKek("unit-test-kek");
        cipher = new CredentialCipher(props);
        service = new TenantCatalogService(repo, cipher, props);
    }

    @Test
    void absentRowResolvesToShared() {
        when(repo.findById("acme")).thenReturn(Optional.empty());
        ResolvedRouting r = service.resolve("acme");
        assertThat(r.shared()).isTrue();
        assertThat(r.routingKey()).isEqualTo(props.getSharedIdentifier());
    }

    @Test
    void nullOrSentinelResolvesToSharedWithoutDbHit() {
        assertThat(service.resolve(null).shared()).isTrue();
        assertThat(service.resolve(props.getSharedIdentifier()).shared()).isTrue();
    }

    @Test
    void sharedActiveRowResolvesToShared() {
        TenantDatasource row = new TenantDatasource();
        row.setTenantId("acme");
        row.setMode(TenantDatasourceMode.SHARED);
        row.setStatus(TenantDatasourceStatus.ACTIVE);
        when(repo.findById("acme")).thenReturn(Optional.of(row));
        assertThat(service.resolve("acme").shared()).isTrue();
    }

    @Test
    void dedicatedActiveResolvesWithDecryptedCreds() {
        TenantDatasource row = new TenantDatasource();
        row.setTenantId("acme");
        row.setMode(TenantDatasourceMode.DEDICATED);
        row.setStatus(TenantDatasourceStatus.ACTIVE);
        row.setJdbcUrlEnc(cipher.encrypt("jdbc:postgresql://acme-db:5432/acme"));
        row.setJdbcUsernameEnc(cipher.encrypt("acme_user"));
        row.setJdbcPasswordEnc(cipher.encrypt("s3cr3t"));
        when(repo.findById("acme")).thenReturn(Optional.of(row));

        ResolvedRouting r = service.resolve("acme");
        assertThat(r.shared()).isFalse();
        assertThat(r.routingKey()).isEqualTo("acme");
        assertThat(r.jdbcUrl()).isEqualTo("jdbc:postgresql://acme-db:5432/acme");
        assertThat(r.username()).isEqualTo("acme_user");
        assertThat(r.password()).isEqualTo("s3cr3t");
        assertThat(r.host()).isEqualTo("acme-db:5432");
    }

    @Test
    void nonActiveStatusFailsClosed() {
        for (TenantDatasourceStatus status : new TenantDatasourceStatus[]{
                TenantDatasourceStatus.PENDING,
                TenantDatasourceStatus.MIGRATING,
                TenantDatasourceStatus.DISABLED}) {
            TenantDatasource row = new TenantDatasource();
            row.setTenantId("t-" + status);
            row.setMode(TenantDatasourceMode.DEDICATED);
            row.setStatus(status);
            when(repo.findById("t-" + status)).thenReturn(Optional.of(row));
            assertThatThrownBy(() -> service.resolve("t-" + status))
                    .isInstanceOf(TenantDatasourceUnavailableException.class)
                    .hasMessageContaining(status.name());
        }
    }

    @Test
    void dedicatedWithMissingCredsFailsClosed() {
        TenantDatasource row = new TenantDatasource();
        row.setTenantId("acme");
        row.setMode(TenantDatasourceMode.DEDICATED);
        row.setStatus(TenantDatasourceStatus.ACTIVE);
        when(repo.findById("acme")).thenReturn(Optional.of(row));
        assertThatThrownBy(() -> service.resolve("acme"))
                .isInstanceOf(TenantDatasourceUnavailableException.class);
    }

    @Test
    void invalidateForcesReread() {
        when(repo.findById("acme")).thenReturn(Optional.empty());
        service.resolve("acme");
        service.invalidate("acme");
        service.resolve("acme");
        // two distinct (uncached) reads
        org.mockito.Mockito.verify(repo, org.mockito.Mockito.times(2)).findById("acme");
    }
}
