package com.csports.infrastructure.redis;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.csports.booking.SessionBookingLock;
import com.csports.booking.exception.BookingOperationInProgressException;

/**
 * Small Redis distributed lock based on SET key value NX PX.
 *
 * The random owner token and compare-and-delete Lua script ensure one request
 * cannot release a lock that expired and was subsequently acquired by another
 * request.
 */
@Component
public class RedisSessionBookingLock implements SessionBookingLock {

    private static final Logger log =
            LoggerFactory.getLogger(RedisSessionBookingLock.class);

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                        return redis.call('del', KEYS[1])
                    end
                    return 0
                    """,
                    Long.class);

    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;
    private final Duration waitTime;
    private final Duration leaseTime;
    private final Duration retryInterval;

    public RedisSessionBookingLock(
            StringRedisTemplate redisTemplate,
            @Value("${csports.booking-lock.enabled:true}") boolean enabled,
            @Value("${csports.booking-lock.wait-time:2s}") Duration waitTime,
            @Value("${csports.booking-lock.lease-time:10s}") Duration leaseTime,
            @Value("${csports.booking-lock.retry-interval:50ms}") Duration retryInterval) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        this.waitTime = requirePositive(waitTime, "wait time");
        this.leaseTime = requirePositive(leaseTime, "lease time");
        this.retryInterval = requirePositive(retryInterval, "retry interval");
    }

    @Override
    public <T> T execute(Long sessionId, Supplier<T> action) {
        if (!enabled) {
            return action.get();
        }

        String key = RedisKeys.BOOKING_LOCK_PREFIX + "{" + sessionId + "}";
        String ownerToken = UUID.randomUUID().toString();
        boolean acquired;

        try {
            acquired = acquire(key, ownerToken);
        } catch (DataAccessException exception) {
            // PostgreSQL still has optimistic locking and hard constraints.
            // Redis improves coordination but is not allowed to become the
            // only correctness mechanism or a single point of failure.
            log.warn(
                    "Redis booking lock unavailable for session {}; "
                            + "continuing with PostgreSQL concurrency protection",
                    sessionId,
                    exception);
            return action.get();
        }

        if (!acquired) {
            throw new BookingOperationInProgressException();
        }

        try {
            return action.get();
        } finally {
            release(key, ownerToken, sessionId);
        }
    }

    private boolean acquire(String key, String ownerToken) {
        long deadline = System.nanoTime() + waitTime.toNanos();

        do {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(key, ownerToken, leaseTime);
            if (Boolean.TRUE.equals(acquired)) {
                return true;
            }

            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                return false;
            }
            LockSupport.parkNanos(Math.min(retryInterval.toNanos(), remaining));
            if (Thread.currentThread().isInterrupted()) {
                throw new BookingOperationInProgressException();
            }
        } while (true);
    }

    private void release(String key, String ownerToken, Long sessionId) {
        try {
            redisTemplate.execute(RELEASE_SCRIPT, List.of(key), ownerToken);
        } catch (DataAccessException exception) {
            // The lease expires automatically. A failed cleanup must not turn
            // an already committed booking into an HTTP failure.
            log.warn(
                    "Could not release Redis booking lock for session {}; "
                            + "the lock will expire after its lease",
                    sessionId,
                    exception);
        }
    }

    private static Duration requirePositive(Duration duration, String propertyName) {
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(
                    "Booking lock " + propertyName + " must be positive.");
        }
        return duration;
    }
}
