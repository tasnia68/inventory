package com.inventory.system.service;

import com.inventory.system.common.entity.SalesRefundStatus;
import com.inventory.system.payload.CreateSalesRefundRequest;
import com.inventory.system.payload.RefundDocumentDto;
import com.inventory.system.payload.RefundStatusDecisionRequest;
import com.inventory.system.payload.SalesRefundDto;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface SalesRefundService {
    SalesRefundDto createRefund(CreateSalesRefundRequest request);

    SalesRefundDto getRefund(UUID id);

    Page<SalesRefundDto> getRefunds(UUID salesOrderId,
                                    UUID customerId,
                                    SalesRefundStatus status,
                                    String refundNumber,
                                    int page,
                                    int size,
                                    String sortBy,
                                    String sortDirection);

    SalesRefundDto approveRefund(UUID id, RefundStatusDecisionRequest request);

    SalesRefundDto rejectRefund(UUID id, RefundStatusDecisionRequest request);

    SalesRefundDto completeRefund(UUID id, RefundStatusDecisionRequest request);

    SalesRefundDto cancelRefund(UUID id, RefundStatusDecisionRequest request);

    RefundDocumentDto generateCreditNote(UUID id);
}