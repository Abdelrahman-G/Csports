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

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
class CacheConfigTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    @Qualifier("sportsCacheValueSerializer")
    private RedisSerializer<List<SportResponse>> sportsCacheValueSerializer;

    @Test
    void cacheManagerShouldExposeExpectedCaches() {
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
        assertThat(cacheManager.getCache("sports")).isNotNull();
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
}
