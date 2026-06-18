package com.inventory.system.config.scaling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Cross-instance lock so that, with multiple backend workers, only one sync of a given
 * (tenant, type) runs at a time. Backed by Redis SET NX when {@code app.scaling.enabled};
 * a no-op (always acquires) in dev/test so behaviour is unchanged without Redis.
 *
 * <p>The lock value is the owning run id, so {@link #release} only frees a lock the caller
 * still owns, and an abandoned lock self-heals after its TTL.
 */
@Slf4j
@Component
public class DistributedLockService {

    private final StringRedisTemplate redis;
    private final boolean enabled;

    public DistributedLockService(ObjectProvider<StringRedisTemplate> redisProvider,
                                  @Value("${app.scaling.enabled:false}") boolean scalingEnabled) {
        this.redis = redisProvider.getIfAvailable();
        this.enabled = scalingEnabled && this.redis != null;
    }

    /** @return true if the lock was acquired (or locking is disabled). */
    public boolean tryAcquire(String key, String owner, Duration ttl) {
        if (!enabled) {
            return true;
        }
        try {
            Boolean ok = redis.opsForValue().setIfAbsent(key, owner, ttl);
            return Boolean.TRUE.equals(ok);
        } catch (RuntimeException ex) {
            // Fail-open: a Redis hiccup must not block syncs entirely.
            log.warn("Lock acquire failed for {}: {} — proceeding without lock", key, ex.getMessage());
            return true;
        }
    }

    public void refresh(String key, Duration ttl) {
        if (!enabled) {
            return;
        }
        try {
            redis.expire(key, ttl);
        } catch (RuntimeException ex) {
            log.debug("Lock refresh failed for {}: {}", key, ex.getMessage());
        }
    }

    public void release(String key, String owner) {
        if (!enabled || owner == null) {
            return;
        }
        try {
            String current = redis.opsForValue().get(key);
            if (owner.equals(current)) {
                redis.delete(key);
            }
        } catch (RuntimeException ex) {
            log.debug("Lock release failed for {}: {}", key, ex.getMessage());
        }
    }
}
