package com.inventory.system.service;

import com.inventory.system.common.entity.Tenant;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringJournalScheduler {

    private final TenantRepository tenantRepository;
    private final AccountingServiceImpl accountingService;

    @Scheduled(cron = "${app.accounting.recurring-journals.cron:0 15 1 * * *}")
    public void runDueRecurringJournals() {
        LocalDate runDate = LocalDate.now();
        tenantRepository.findAll().stream()
                .filter(tenant -> tenant.getStatus() == Tenant.TenantStatus.ACTIVE)
                .forEach(tenant -> TenantContext.runWithTenant(tenant.getId().toString(), () -> {
                    try {
                        int created = accountingService.runDueRecurringJournalTemplatesForTenant(tenant.getId().toString(), runDate).size();
                        if (created > 0) {
                            log.info("Created {} recurring journal entries for tenant {}", created, tenant.getId());
                        }
                    } catch (RuntimeException ex) {
                        log.warn("Recurring journal run failed for tenant {}: {}", tenant.getId(), ex.getMessage());
                    }
                }));
    }
}
