package com.inventory.system.entity;

import com.inventory.system.common.entity.ProductAttribute;
import com.inventory.system.common.entity.ProductTemplate;
import com.inventory.system.service.ProductService;
import com.inventory.system.service.ProductServiceImpl;
import com.inventory.system.common.entity.ProductVariant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ProductEntityTest {

    @Test
    public void testProductTemplateCreation() {
        ProductTemplate template = new ProductTemplate();
        template.setName("T-Shirt");
        template.setDescription("Basic Cotton T-Shirt");

        assertEquals("T-Shirt", template.getName());
        assertEquals("Basic Cotton T-Shirt", template.getDescription());
        assertTrue(template.getIsActive());
    }

    @Test
    public void testProductAttributeCreation() {
        ProductAttribute attribute = new ProductAttribute();
        attribute.setName("Size");
        attribute.setType(ProductAttribute.AttributeType.DROPDOWN);
        attribute.setRequired(true);

        assertEquals("Size", attribute.getName());
        assertEquals(ProductAttribute.AttributeType.DROPDOWN, attribute.getType());
        assertTrue(attribute.getRequired());
    }

    @Test
    public void testSkuGeneration() {
        ProductTemplate template = new ProductTemplate();
        template.setName("Laptop");

        ProductVariant variant = new ProductVariant();
        variant.setTemplate(template);

        ProductService productService = new ProductServiceImpl();
        String sku = productService.generateSku(variant);

        assertNotNull(sku);
        assertTrue(sku.startsWith("LAP-"));
    }
}
