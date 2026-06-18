package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Journal of a chunked Shopify sync. One row per sync invocation; the frontend drives
 * it one page at a time via processNextPage, persisting the GraphQL endCursor after each
 * page so a failed run can resume from exactly where it stopped instead of restarting.
 */
@Entity
@Table(name = "shopify_sync_runs")
@Getter
@Setter
public class ShopifySyncRun extends BaseEntity {

    /** PRODUCTS | ORDERS | INVENTORY | LOCATIONS */
    @Column(name = "sync_type", nullable = false, length = 32)
    private String syncType;

    /** RUNNING | COMPLETED | FAILED */
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    /** Last GraphQL endCursor successfully processed; null = start from the beginning. */
    @Column(name = "cursor", columnDefinition = "TEXT")
    private String cursor;

    @Column(name = "incremental", nullable = false)
    private boolean incremental = false;

    /** The Shopify search filter applied (e.g. updated_at:>'...'); null = full pull. */
    @Column(name = "query_filter", columnDefinition = "TEXT")
    private String queryFilter;

    /** UTC instant captured at start; promoted to the since-watermark when the run COMPLETES. */
    @Column(name = "next_watermark", length = 64)
    private String nextWatermark;

    @Column(name = "pages_processed", nullable = false)
    private int pagesProcessed = 0;

    /** Serialized ShopifySyncResultDto running totals. */
    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
}
