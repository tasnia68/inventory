package com.inventory.system.service;

import com.inventory.system.common.entity.ProductVariant;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {

    @Override
    public String generateSku(ProductVariant variant) {
        // Simple SKU generation logic: TEMPLATE_NAME-RANDOM_UUID_SUBSTRING
        // In a real scenario, this would be more complex, possibly involving attributes.
        String templateName = variant.getTemplate() != null ? variant.getTemplate().getName() : "UNK";
        String prefix = templateName.length() > 3 ? templateName.substring(0, 3).toUpperCase() : templateName.toUpperCase();
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
