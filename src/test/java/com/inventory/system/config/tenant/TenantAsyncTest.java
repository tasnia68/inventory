package com.inventory.system.config.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class TenantAsyncTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void completableFutureSupplyAsyncCarriesSubmittingThreadTenantIntoCommonPool() throws Exception {
        TenantContext.setTenantId("tenant-a");

        CompletableFuture<String> future = TenantAsync.supplyAsync(TenantContext::getTenantId);

        TenantContext.clear();

        Assertions.assertEquals("tenant-a", future.get(5, TimeUnit.SECONDS));
        Assertions.assertNull(TenantContext.getCurrentTenantId());
    }

    @Test
    void completableFutureSupplyAsyncWithExecutorRestoresWorkerThreadAfterTaskFinishes() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            TenantContext.setTenantId("tenant-b");

            CompletableFuture<String> future = TenantAsync.supplyAsync(TenantContext::getTenantId, executor);

            TenantContext.clear();

            Assertions.assertEquals("tenant-b", future.get(5, TimeUnit.SECONDS));

            Future<String> verifyWorkerIsClean = executor.submit(TenantContext::getCurrentTenantId);
            Assertions.assertNull(verifyWorkerIsClean.get(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void forkJoinPoolSubmissionCarriesTenantAndDoesNotLeakItAfterCompletion() throws Exception {
        ForkJoinPool pool = new ForkJoinPool(1);
        try {
            TenantContext.setTenantId("tenant-c");

            ForkJoinTask<String> task = TenantAsync.submit(pool, TenantContext::getTenantId);

            TenantContext.clear();

            Assertions.assertEquals("tenant-c", task.get(5, TimeUnit.SECONDS));
            Assertions.assertNull(pool.submit(TenantContext::getCurrentTenantId).get(5, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }
}