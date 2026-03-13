package com.inventory.system.controller;

import com.inventory.system.common.entity.CustomerCategory;
import com.inventory.system.common.entity.CustomerStatus;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.AdjustCustomerCreditRequest;
import com.inventory.system.payload.CreateCustomerRequest;
import com.inventory.system.payload.CustomerDto;
import com.inventory.system.payload.CustomerCreditTransactionDto;
import com.inventory.system.payload.CustomerOrderHistoryDto;
import com.inventory.system.payload.CustomerPriceListDto;
import com.inventory.system.payload.CustomerPriceListRequest;
import com.inventory.system.payload.UpdateCustomerRequest;
import com.inventory.system.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerDto>> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerDto customer = customerService.createCustomer(request);
        ApiResponse<CustomerDto> response = new ApiResponse<>(true, "Customer created successfully", customer);
        response.setStatus(201);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDto>> getCustomerById(@PathVariable UUID id) {
        CustomerDto customer = customerService.getCustomerById(id);
        ApiResponse<CustomerDto> response = new ApiResponse<>(true, "Customer retrieved successfully", customer);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerDto>>> getAllCustomers(
            @RequestParam(required = false) CustomerCategory category,
            @RequestParam(required = false) CustomerStatus status) {
        List<CustomerDto> customers = customerService.getAllCustomers(category, status);
        ApiResponse<List<CustomerDto>> response = new ApiResponse<>(true, "Customers retrieved successfully", customers);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDto>> updateCustomer(@PathVariable UUID id, @Valid @RequestBody UpdateCustomerRequest request) {
        CustomerDto customer = customerService.updateCustomer(id, request);
        ApiResponse<CustomerDto> response = new ApiResponse<>(true, "Customer updated successfully", customer);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable UUID id) {
        customerService.deleteCustomer(id);
        ApiResponse<Void> response = new ApiResponse<>(true, "Customer deleted successfully", null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/price-lists")
    public ResponseEntity<ApiResponse<CustomerPriceListDto>> createPriceList(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerPriceListRequest request) {
        CustomerPriceListDto dto = customerService.createPriceList(id, request);
        ApiResponse<CustomerPriceListDto> response = new ApiResponse<>(true, "Customer price list created successfully", dto);
        response.setStatus(201);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/price-lists/{priceListId}")
    public ResponseEntity<ApiResponse<CustomerPriceListDto>> updatePriceList(
            @PathVariable UUID id,
            @PathVariable UUID priceListId,
            @Valid @RequestBody CustomerPriceListRequest request) {
        CustomerPriceListDto dto = customerService.updatePriceList(id, priceListId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Customer price list updated successfully", dto));
    }

    @GetMapping("/{id}/price-lists")
    public ResponseEntity<ApiResponse<List<CustomerPriceListDto>>> getPriceLists(@PathVariable UUID id) {
        List<CustomerPriceListDto> data = customerService.getPriceLists(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Customer price lists retrieved successfully", data));
    }

    @DeleteMapping("/{id}/price-lists/{priceListId}")
    public ResponseEntity<ApiResponse<Void>> deletePriceList(@PathVariable UUID id, @PathVariable UUID priceListId) {
        customerService.deletePriceList(id, priceListId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Customer price list deleted successfully", null));
    }

    @PostMapping("/{id}/credit/adjust")
    public ResponseEntity<ApiResponse<CustomerCreditTransactionDto>> adjustCredit(
            @PathVariable UUID id,
            @Valid @RequestBody AdjustCustomerCreditRequest request) {
        CustomerCreditTransactionDto dto = customerService.adjustCredit(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Customer credit adjusted successfully", dto));
    }

    @GetMapping("/{id}/credit/transactions")
    public ResponseEntity<ApiResponse<Page<CustomerCreditTransactionDto>>> getCreditTransactions(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        Page<CustomerCreditTransactionDto> data = customerService.getCreditTransactions(id, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(new ApiResponse<>(true, "Customer credit transactions retrieved successfully", data));
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<ApiResponse<Page<CustomerOrderHistoryDto>>> getCustomerOrders(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        Page<CustomerOrderHistoryDto> data = customerService.getCustomerOrderHistory(id, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(new ApiResponse<>(true, "Customer order history retrieved successfully", data));
    }
}
