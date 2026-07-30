package com.csports.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.test.context.ActiveProfiles;

import com.csports.sport.dto.SportResponse;
import com.csports.common.pagination.PageResponse;
import com.csports.session.TrainingSessionStatus;
import com.csports.session.dto.TrainingSessionResponse;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@SpringBootTest(properties = "csports.cache.enabled=true")
@ActiveProfiles("test")
class CacheConfigTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    @Qualifier("sportsCacheValueSerializer")
    private RedisSerializer<List<SportResponse>> sportsCacheValueSerializer;

    @Autowired
    @Qualifier("sessionSearchCacheValueSerializer")
    private RedisSerializer<PageResponse<TrainingSessionResponse>>
            sessionSearchCacheValueSerializer;

    @Test
    void cacheManagerShouldExposeExpectedCaches() {
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
        assertThat(cacheManager.getCache("sports")).isNotNull();
        assertThat(cacheManager.getCache("session-search")).isNotNull();
    }

    @Test
    void redisSerializerShouldPreserveSportResponseTypeInsideAList() {
        List<SportResponse> sports = List.of(new SportResponse(1L, "Football"));

        byte[] serialized = sportsCacheValueSerializer.serialize(sports);
        List<SportResponse> deserialized = sportsCacheValueSerializer.deserialize(serialized);

        assertThat(deserialized).isInstanceOf(List.class);
        assertThat(deserialized)
                .singleElement()
                .isInstanceOf(SportResponse.class)
                .isEqualTo(sports.getFirst());
    }

    @Test
    void sessionSearchSerializerPreservesNestedResponseTypes() {
        TrainingSessionResponse session = new TrainingSessionResponse(
                5L,
                "Cached swimming",
                7L,
                "Trainer",
                2L,
                "Swimming",
                "Cairo Club",
                1L,
                "Nasr City",
                "Cairo",
                "Egypt",
                "https://www.google.com/maps?q=30.05,31.25",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 12, 1),
                LocalTime.of(9, 0),
                120,
                Set.of(DayOfWeek.MONDAY),
                700.0,
                1,
                5,
                4,
                LocalDate.of(2026, 10, 5).atTime(9, 0),
                true,
                TrainingSessionStatus.SCHEDULED,
                null,
                null);
        PageResponse<TrainingSessionResponse> page = new PageResponse<>(
                List.of(session),
                0,
                10,
                1,
                1,
                true,
                true);

        byte[] serialized = sessionSearchCacheValueSerializer.serialize(page);
        PageResponse<TrainingSessionResponse> deserialized =
                sessionSearchCacheValueSerializer.deserialize(serialized);

        assertThat(deserialized).isNotNull();
        assertThat(deserialized.content())
                .singleElement()
                .isInstanceOf(TrainingSessionResponse.class)
                .isEqualTo(session);
    }
}
