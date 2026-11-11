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
import com.csports.course.TrainingCourseStatus;
import com.csports.course.dto.TrainingCourseResponse;

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
    @Qualifier("courseSearchCacheValueSerializer")
    private RedisSerializer<PageResponse<TrainingCourseResponse>>
            courseSearchCacheValueSerializer;

    @Test
    void cacheManagerShouldExposeExpectedCaches() {
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
        assertThat(cacheManager.getCache("sports")).isNotNull();
        assertThat(cacheManager.getCache("course-search")).isNotNull();
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
    void courseSearchSerializerPreservesNestedResponseTypes() {
        TrainingCourseResponse course = new TrainingCourseResponse(
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
                TrainingCourseStatus.SCHEDULED,
                null,
                null,
                null);
        PageResponse<TrainingCourseResponse> page = new PageResponse<>(
                List.of(course),
                0,
                10,
                1,
                1,
                true,
                true);

        byte[] serialized = courseSearchCacheValueSerializer.serialize(page);
        PageResponse<TrainingCourseResponse> deserialized =
                courseSearchCacheValueSerializer.deserialize(serialized);

        assertThat(deserialized).isNotNull();
        assertThat(deserialized.content())
                .singleElement()
                .isInstanceOf(TrainingCourseResponse.class)
                .isEqualTo(course);
    }
}
