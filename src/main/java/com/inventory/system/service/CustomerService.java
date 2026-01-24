package com.inventory.system.service;

import com.inventory.system.payload.CreateCustomerRequest;
import com.inventory.system.payload.CustomerDto;
import com.inventory.system.payload.UpdateCustomerRequest;

import java.util.List;
import java.util.UUID;

public interface CustomerService {
    CustomerDto createCustomer(CreateCustomerRequest request);
    CustomerDto updateCustomer(UUID id, UpdateCustomerRequest request);
    CustomerDto getCustomerById(UUID id);
    List<CustomerDto> getAllCustomers();
    void deleteCustomer(UUID id);
}
