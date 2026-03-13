package com.inventory.system.repository;

import com.inventory.system.common.entity.Customer;
import com.inventory.system.common.entity.CustomerCategory;
import com.inventory.system.common.entity.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {
	List<Customer> findByCategory(CustomerCategory category);
	List<Customer> findByStatus(CustomerStatus status);
}
