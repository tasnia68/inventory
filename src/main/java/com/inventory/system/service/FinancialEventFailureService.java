package com.inventory.system.service;

import com.inventory.system.common.entity.PostingStatus;
import com.inventory.system.repository.FinancialEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinancialEventFailureService {

    private final FinancialEventRepository financialEventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID financialEventId, String failureReason) {
        financialEventRepository.findById(financialEventId).ifPresent(financialEvent -> {
            if (financialEvent.getPostingStatus() == PostingStatus.POSTED) {
                return;
            }
            financialEvent.setPostingStatus(PostingStatus.FAILED);
            financialEvent.setFailureReason(failureReason);
            financialEvent.getSubledgerEntries().forEach(line -> line.setPostingStatus(PostingStatus.FAILED));
            financialEventRepository.save(financialEvent);
        });
    }
}
