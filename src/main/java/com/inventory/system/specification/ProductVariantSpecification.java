package com.inventory.system.specification;

import com.inventory.system.common.entity.ProductAttributeValue;
import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.payload.ProductSearchDto;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProductVariantSpecification {

    public static Specification<ProductVariant> getSpecification(ProductSearchDto searchDto) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchDto.getTemplateId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("template").get("id"), searchDto.getTemplateId()));
            }

            if (searchDto.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), searchDto.getMinPrice()));
            }

            if (searchDto.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), searchDto.getMaxPrice()));
            }

            if (searchDto.getSku() != null && !searchDto.getSku().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("sku")), "%" + searchDto.getSku().toLowerCase() + "%"));
            }

            // Dynamic Attribute Filtering (using subqueries for AND logic across multiple attributes)
            if (searchDto.getAttributes() != null && !searchDto.getAttributes().isEmpty()) {
                 for (Map.Entry<String, String> entry : searchDto.getAttributes().entrySet()) {
                    String attributeName = entry.getKey();
                    String attributeValue = entry.getValue();

                    // Subquery to check if the variant has a specific attribute value
                    // exists (select 1 from ProductAttributeValue pav join pav.attribute pa
                    //         where pav.variant = root and pa.name = :name and pav.value = :value)

                    var subquery = query.subquery(Long.class);
                    var subRoot = subquery.from(ProductAttributeValue.class);
                    subquery.select(criteriaBuilder.literal(1L));

                    Predicate variantMatch = criteriaBuilder.equal(subRoot.get("variant"), root);
                    Predicate nameMatch = criteriaBuilder.equal(subRoot.get("attribute").get("name"), attributeName);
                    Predicate valueMatch = criteriaBuilder.like(criteriaBuilder.lower(subRoot.get("value")), "%" + attributeValue.toLowerCase() + "%");

                    subquery.where(variantMatch, nameMatch, valueMatch);

                    predicates.add(criteriaBuilder.exists(subquery));
                 }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
