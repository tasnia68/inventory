package com.inventory.system.service;

import com.inventory.system.common.entity.CreditTransactionType;
import com.inventory.system.common.entity.Customer;
import com.inventory.system.common.entity.CustomerCategory;
import com.inventory.system.common.entity.CustomerCreditTransaction;
import com.inventory.system.common.entity.CustomerPriceList;
import com.inventory.system.common.entity.CustomerStatus;
import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.SalesOrder;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.AdjustCustomerCreditRequest;
import com.inventory.system.payload.CreateCustomerRequest;
import com.inventory.system.payload.CustomerDto;
import com.inventory.system.payload.CustomerCreditTransactionDto;
import com.inventory.system.payload.CustomerOrderHistoryDto;
import com.inventory.system.payload.CustomerPriceListDto;
import com.inventory.system.payload.CustomerPriceListRequest;
import com.inventory.system.payload.UpdateCustomerRequest;
import com.inventory.system.repository.CustomerCreditTransactionRepository;
import com.inventory.system.repository.CustomerPriceListRepository;
import com.inventory.system.repository.CustomerRepository;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CustomerPriceListRepository customerPriceListRepository;
    private final CustomerCreditTransactionRepository customerCreditTransactionRepository;
    private final SalesOrderRepository salesOrderRepository;

    @Override
    @Transactional
    public CustomerDto createCustomer(CreateCustomerRequest request) {
        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setContactName(request.getContactName());
        customer.setEmail(request.getEmail());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setAddress(request.getAddress());
        customer.setCreditLimit(request.getCreditLimit());
        customer.setCategory(request.getCategory() != null ? request.getCategory() : CustomerCategory.OTHER);
        customer.setOutstandingBalance(BigDecimal.ZERO);
        if (request.getIsActive() != null) {
            customer.setIsActive(request.getIsActive());
        }
        if (request.getStatus() != null) {
            customer.setStatus(request.getStatus());
        }

        Customer savedCustomer = customerRepository.save(customer);
        return mapToDto(savedCustomer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDto getCustomerById(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        return mapToDto(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDto> getAllCustomers() {
        return getAllCustomers(null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDto> getAllCustomers(CustomerCategory category, CustomerStatus status) {
        if (category != null) {
            return customerRepository.findByCategory(category).stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        }

        if (status != null) {
            return customerRepository.findByStatus(status).stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        }

        return customerRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomerDto updateCustomer(UUID id, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        if (request.getName() != null) {
            customer.setName(request.getName());
        }
        if (request.getContactName() != null) {
            customer.setContactName(request.getContactName());
        }
        if (request.getEmail() != null) {
            customer.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            customer.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            customer.setAddress(request.getAddress());
        }
        if (request.getCreditLimit() != null) {
            customer.setCreditLimit(request.getCreditLimit());
            if (customer.getOutstandingBalance() != null && customer.getOutstandingBalance().compareTo(request.getCreditLimit()) > 0) {
                customer.setStatus(CustomerStatus.BLOCKED);
            }
        }
        if (request.getCategory() != null) {
            customer.setCategory(request.getCategory());
        }
        if (request.getIsActive() != null) {
            customer.setIsActive(request.getIsActive());
        }
        if (request.getStatus() != null) {
            customer.setStatus(request.getStatus());
        }

        Customer updatedCustomer = customerRepository.save(customer);
        return mapToDto(updatedCustomer);
    }

    @Override
    @Transactional
    public void deleteCustomer(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        customerRepository.delete(customer);
    }

    @Override
    @Transactional
    public CustomerPriceListDto createPriceList(UUID customerId, CustomerPriceListRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        ProductVariant variant = productVariantRepository.findById(request.getProductVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with id: " + request.getProductVariantId()));

        validatePriceListDates(request.getEffectiveFrom(), request.getEffectiveTo());

        CustomerPriceList priceList = new CustomerPriceList();
        priceList.setCustomer(customer);
        priceList.setProductVariant(variant);
        priceList.setPrice(request.getPrice());
        priceList.setCurrency(request.getCurrency());
        priceList.setEffectiveFrom(request.getEffectiveFrom());
        priceList.setEffectiveTo(request.getEffectiveTo());
        priceList.setNotes(request.getNotes());

        return mapPriceListToDto(customerPriceListRepository.save(priceList));
    }

    @Override
    @Transactional
    public CustomerPriceListDto updatePriceList(UUID customerId, UUID priceListId, CustomerPriceListRequest request) {
        CustomerPriceList priceList = customerPriceListRepository.findById(priceListId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer price list not found with id: " + priceListId));

        if (!priceList.getCustomer().getId().equals(customerId)) {
            throw new BadRequestException("Price list does not belong to customer");
        }

        if (request.getProductVariantId() != null && !request.getProductVariantId().equals(priceList.getProductVariant().getId())) {
            ProductVariant variant = productVariantRepository.findById(request.getProductVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with id: " + request.getProductVariantId()));
            priceList.setProductVariant(variant);
        }

        if (request.getPrice() != null) {
            priceList.setPrice(request.getPrice());
        }
        if (request.getCurrency() != null) {
            priceList.setCurrency(request.getCurrency());
        }
        if (request.getEffectiveFrom() != null) {
            priceList.setEffectiveFrom(request.getEffectiveFrom());
        }
        if (request.getEffectiveTo() != null) {
            priceList.setEffectiveTo(request.getEffectiveTo());
        }
        if (request.getNotes() != null) {
            priceList.setNotes(request.getNotes());
        }

        validatePriceListDates(priceList.getEffectiveFrom(), priceList.getEffectiveTo());

        return mapPriceListToDto(customerPriceListRepository.save(priceList));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerPriceListDto> getPriceLists(UUID customerId) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        return customerPriceListRepository.findByCustomerId(customerId).stream()
                .map(this::mapPriceListToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePriceList(UUID customerId, UUID priceListId) {
        CustomerPriceList priceList = customerPriceListRepository.findById(priceListId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer price list not found with id: " + priceListId));

        if (!priceList.getCustomer().getId().equals(customerId)) {
            throw new BadRequestException("Price list does not belong to customer");
        }

        customerPriceListRepository.delete(priceList);
    }

    @Override
    @Transactional
    public CustomerCreditTransactionDto adjustCredit(UUID customerId, AdjustCustomerCreditRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        BigDecimal currentBalance = customer.getOutstandingBalance() != null ? customer.getOutstandingBalance() : BigDecimal.ZERO;
        BigDecimal newBalance;

        if (request.getType() == CreditTransactionType.CHARGE || request.getType() == CreditTransactionType.ADJUSTMENT) {
            newBalance = currentBalance.add(request.getAmount());
        } else {
            newBalance = currentBalance.subtract(request.getAmount());
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                newBalance = BigDecimal.ZERO;
            }
        }

        if (request.getType() == CreditTransactionType.CHARGE && customer.getCreditLimit() != null && newBalance.compareTo(customer.getCreditLimit()) > 0) {
            throw new BadRequestException("Credit limit exceeded. Limit: " + customer.getCreditLimit() + ", attempted balance: " + newBalance);
        }

        customer.setOutstandingBalance(newBalance);
        if (customer.getCreditLimit() != null && newBalance.compareTo(customer.getCreditLimit()) > 0) {
            customer.setStatus(CustomerStatus.BLOCKED);
        } else if (customer.getStatus() == CustomerStatus.BLOCKED) {
            customer.setStatus(CustomerStatus.ACTIVE);
        }
        customerRepository.save(customer);

        CustomerCreditTransaction tx = new CustomerCreditTransaction();
        tx.setCustomer(customer);
        tx.setType(request.getType());
        tx.setAmount(request.getAmount());
        tx.setBalanceBefore(currentBalance);
        tx.setBalanceAfter(newBalance);
        tx.setReferenceNumber(request.getReferenceNumber());
        tx.setNotes(request.getNotes());
        tx.setTransactionDate(LocalDateTime.now());

        return mapCreditTxToDto(customerCreditTransactionRepository.save(tx));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerCreditTransactionDto> getCreditTransactions(UUID customerId, int page, int size, String sortBy, String sortDirection) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return customerCreditTransactionRepository.findByCustomerId(customerId, pageable).map(this::mapCreditTxToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerOrderHistoryDto> getCustomerOrderHistory(UUID customerId, int page, int size, String sortBy, String sortDirection) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<SalesOrder> orders = salesOrderRepository.findByCustomerId(customerId, pageable);

        return orders.map(order -> {
            CustomerOrderHistoryDto dto = new CustomerOrderHistoryDto();
            dto.setSalesOrderId(order.getId());
            dto.setSoNumber(order.getSoNumber());
            dto.setOrderDate(order.getOrderDate());
            dto.setStatus(order.getStatus());
            dto.setTotalAmount(order.getTotalAmount());
            dto.setCurrency(order.getCurrency());
            if (order.getWarehouse() != null) {
                dto.setWarehouseId(order.getWarehouse().getId());
                dto.setWarehouseName(order.getWarehouse().getName());
            }
            return dto;
        });
    }

    private CustomerDto mapToDto(Customer customer) {
        CustomerDto dto = new CustomerDto();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setContactName(customer.getContactName());
        dto.setEmail(customer.getEmail());
        dto.setPhoneNumber(customer.getPhoneNumber());
        dto.setAddress(customer.getAddress());
        dto.setCreditLimit(customer.getCreditLimit());
        dto.setOutstandingBalance(customer.getOutstandingBalance() != null ? customer.getOutstandingBalance() : BigDecimal.ZERO);
        if (customer.getCreditLimit() != null) {
            dto.setAvailableCredit(customer.getCreditLimit().subtract(dto.getOutstandingBalance()));
        }
        dto.setCategory(customer.getCategory());
        dto.setIsActive(customer.getIsActive());
        dto.setStatus(customer.getStatus());
        dto.setCreatedAt(customer.getCreatedAt());
        dto.setUpdatedAt(customer.getUpdatedAt());
        return dto;
    }

    private CustomerPriceListDto mapPriceListToDto(CustomerPriceList entity) {
        CustomerPriceListDto dto = new CustomerPriceListDto();
        dto.setId(entity.getId());
        dto.setCustomerId(entity.getCustomer().getId());
        dto.setProductVariantId(entity.getProductVariant().getId());
        dto.setSku(entity.getProductVariant().getSku());
        dto.setPrice(entity.getPrice());
        dto.setCurrency(entity.getCurrency());
        dto.setEffectiveFrom(entity.getEffectiveFrom());
        dto.setEffectiveTo(entity.getEffectiveTo());
        dto.setNotes(entity.getNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private CustomerCreditTransactionDto mapCreditTxToDto(CustomerCreditTransaction entity) {
        CustomerCreditTransactionDto dto = new CustomerCreditTransactionDto();
        dto.setId(entity.getId());
        dto.setCustomerId(entity.getCustomer().getId());
        dto.setType(entity.getType());
        dto.setAmount(entity.getAmount());
        dto.setBalanceBefore(entity.getBalanceBefore());
        dto.setBalanceAfter(entity.getBalanceAfter());
        dto.setReferenceNumber(entity.getReferenceNumber());
        dto.setNotes(entity.getNotes());
        dto.setTransactionDate(entity.getTransactionDate());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private void validatePriceListDates(LocalDate effectiveFrom, LocalDate effectiveTo) {
        if (effectiveFrom != null && effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new BadRequestException("effectiveTo must be equal or after effectiveFrom");
        }
    }
}
