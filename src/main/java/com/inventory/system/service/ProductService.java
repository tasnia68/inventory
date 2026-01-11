package com.inventory.system.service;

import com.inventory.system.common.entity.ProductVariant;

public interface ProductService {
    String generateSku(ProductVariant variant);
}
