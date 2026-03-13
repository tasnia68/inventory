package com.inventory.system.repository;

import com.inventory.system.common.entity.CustomerPriceList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomerPriceListRepository extends JpaRepository<CustomerPriceList, UUID> {
    List<CustomerPriceList> findByCustomerId(UUID customerId);
}