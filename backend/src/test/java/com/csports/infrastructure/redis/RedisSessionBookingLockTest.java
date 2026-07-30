package com.csports.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.csports.booking.exception.BookingOperationInProgressException;

@ExtendWith(MockitoExtension.class)
class RedisSessionBookingLockTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void unavailableRedisFallsBackToTheDatabaseProtectedAction() {
        when(valueOperations.setIfAbsent(
                anyString(),
                anyString(),
                any(Duration.class)))
                .thenThrow(new DataAccessResourceFailureException("Redis unavailable"));

        RedisSessionBookingLock lock = new RedisSessionBookingLock(
                redisTemplate,
                true,
                Duration.ofMillis(5),
                Duration.ofSeconds(1),
                Duration.ofMillis(1));

        assertThat(lock.execute(42L, () -> "database-result"))
                .isEqualTo("database-result");
    }

    @Test
    void busyLockReturnsConflictAfterItsConfiguredWait() {
        when(valueOperations.setIfAbsent(
                anyString(),
                anyString(),
                any(Duration.class)))
                .thenReturn(false);

        RedisSessionBookingLock lock = new RedisSessionBookingLock(
                redisTemplate,
                true,
                Duration.ofMillis(1),
                Duration.ofSeconds(1),
                Duration.ofMillis(1));

        assertThatThrownBy(() -> lock.execute(42L, () -> "not-executed"))
                .isInstanceOf(BookingOperationInProgressException.class);
    }
}
