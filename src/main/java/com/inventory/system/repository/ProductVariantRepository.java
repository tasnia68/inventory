package com.inventory.system.repository;

import com.inventory.system.common.entity.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID>, JpaSpecificationExecutor<ProductVariant> {
    Optional<ProductVariant> findBySku(String sku);
    boolean existsBySku(String sku);
    List<ProductVariant> findByTemplateId(UUID templateId);

    @Query("select v from ProductVariant v join v.template t " +
            "where lower(v.sku) like lower(concat('%', :q, '%')) " +
            "or lower(coalesce(v.barcode, '')) like lower(concat('%', :q, '%')) " +
            "or lower(t.name) like lower(concat('%', :q, '%'))")
    Page<ProductVariant> searchByQuery(@Param("q") String query, Pageable pageable);

        @Query("select distinct v from ProductVariant v " +
            "join v.template t " +
            "left join v.attributeValues pav " +
            "left join pav.attribute pa " +
            "where (:q is null or :q = '' or " +
            "lower(v.sku) like lower(concat('%', :q, '%')) or " +
            "lower(coalesce(v.barcode, '')) like lower(concat('%', :q, '%')) or " +
            "lower(t.name) like lower(concat('%', :q, '%'))) " +
            "and (:categoryId is null or t.category.id = :categoryId) " +
            "and (:templateId is null or t.id = :templateId) " +
            "and (:attributeId is null or pa.id = :attributeId) " +
            "and (:attributeValue is null or :attributeValue = '' or lower(coalesce(pav.value, '')) like lower(concat('%', :attributeValue, '%')))")
        Page<ProductVariant> searchAdvanced(
            @Param("q") String q,
            @Param("categoryId") UUID categoryId,
            @Param("templateId") UUID templateId,
            @Param("attributeId") UUID attributeId,
            @Param("attributeValue") String attributeValue,
            Pageable pageable);

        Page<ProductVariant> findByTemplateCategoryId(UUID categoryId, Pageable pageable);
}
