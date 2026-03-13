package com.inventory.system.service;

import com.inventory.system.payload.CreateCustomerRequest;
import com.inventory.system.payload.CustomerDto;
import com.inventory.system.payload.CustomerPriceListDto;
import com.inventory.system.payload.CustomerPriceListRequest;
import com.inventory.system.payload.AdjustCustomerCreditRequest;
import com.inventory.system.payload.CustomerCreditTransactionDto;
import com.inventory.system.payload.CustomerOrderHistoryDto;
import com.inventory.system.payload.UpdateCustomerRequest;
import com.inventory.system.common.entity.CustomerCategory;
import com.inventory.system.common.entity.CustomerStatus;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface CustomerService {
    CustomerDto createCustomer(CreateCustomerRequest request);
    CustomerDto updateCustomer(UUID id, UpdateCustomerRequest request);
    CustomerDto getCustomerById(UUID id);
    List<CustomerDto> getAllCustomers(CustomerCategory category, CustomerStatus status);
    List<CustomerDto> getAllCustomers();
    void deleteCustomer(UUID id);

    CustomerPriceListDto createPriceList(UUID customerId, CustomerPriceListRequest request);
    CustomerPriceListDto updatePriceList(UUID customerId, UUID priceListId, CustomerPriceListRequest request);
    List<CustomerPriceListDto> getPriceLists(UUID customerId);
    void deletePriceList(UUID customerId, UUID priceListId);

    CustomerCreditTransactionDto adjustCredit(UUID customerId, AdjustCustomerCreditRequest request);
    Page<CustomerCreditTransactionDto> getCreditTransactions(UUID customerId, int page, int size, String sortBy, String sortDirection);

    Page<CustomerOrderHistoryDto> getCustomerOrderHistory(UUID customerId, int page, int size, String sortBy, String sortDirection);
}
