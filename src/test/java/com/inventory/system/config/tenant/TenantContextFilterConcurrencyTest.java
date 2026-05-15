package com.inventory.system.config.tenant;

import com.inventory.system.security.JwtService;
import com.inventory.system.service.StorefrontDomainService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantContextFilterConcurrencyTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void concurrentRequestsKeepIndependentTenantContextAndClearAfterCompletion() throws Exception {
        TenantContextFilter filter = new TenantContextFilter(
                entityManagerWithTenantFilter(),
                mock(StorefrontDomainService.class),
                mock(JwtService.class));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<RequestObservation> tenantA = executor.submit(buildRequestTask(filter, "tenant-a", ready, start));
            Future<RequestObservation> tenantB = executor.submit(buildRequestTask(filter, "tenant-b", ready, start));

            Assertions.assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            List<RequestObservation> observations = new ArrayList<>();
            observations.add(tenantA.get(5, TimeUnit.SECONDS));
            observations.add(tenantB.get(5, TimeUnit.SECONDS));

            Assertions.assertTrue(observations.contains(new RequestObservation("tenant-a", null)));
            Assertions.assertTrue(observations.contains(new RequestObservation("tenant-b", null)));
            Assertions.assertNull(TenantContext.getCurrentTenantId());
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<RequestObservation> buildRequestTask(
            TenantContextFilter filter,
            String tenantId,
            CountDownLatch ready,
            CountDownLatch start) {
        return () -> {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/settings");
            request.addHeader(TenantContextFilter.TENANT_HEADER, tenantId);
            MockHttpServletResponse response = new MockHttpServletResponse();
            String[] seenTenant = new String[1];
            FilterChain chain = (servletRequest, servletResponse) -> {
                ready.countDown();
                awaitStart(start);
                seenTenant[0] = TenantContext.getTenantId();
            };

            filter.doFilter(request, response, chain);

            Assertions.assertEquals(200, response.getStatus());
            return new RequestObservation(seenTenant[0], TenantContext.getCurrentTenantId());
        };
    }

    private EntityManager entityManagerWithTenantFilter() {
        EntityManager entityManager = mock(EntityManager.class);
        Session session = mock(Session.class);
        Filter hibernateFilter = mock(Filter.class);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter("tenantFilter")).thenReturn(hibernateFilter);
        when(hibernateFilter.setParameter(anyString(), anyString())).thenReturn(hibernateFilter);
        return entityManager;
    }

    private void awaitStart(CountDownLatch start) {
        try {
            Assertions.assertTrue(start.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for concurrent request start", exception);
        }
    }

    private record RequestObservation(String tenantDuringRequest, String tenantAfterRequest) {
    }
}