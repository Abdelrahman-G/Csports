package com.csports.infrastructure.redis;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.csports.common.pagination.PageResponse;
import com.csports.session.dto.TrainingSessionResponse;
import com.csports.sport.dto.SportResponse;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class RedisConfig implements CachingConfigurer {

    private final RedisCacheErrorHandler redisCacheErrorHandler;

    public RedisConfig(RedisCacheErrorHandler redisCacheErrorHandler) {
        this.redisCacheErrorHandler = redisCacheErrorHandler;
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return redisCacheErrorHandler;
    }

    @Bean("redisValueSerializer")
    public RedisSerializer<Object> redisValueSerializer(ObjectMapper objectMapper) {
        return new GenericJacksonJsonRedisSerializer(objectMapper);
    }

    @Bean("sportsCacheValueSerializer")
    public RedisSerializer<List<SportResponse>> sportsCacheValueSerializer(ObjectMapper objectMapper) {
        JavaType sportsListType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, SportResponse.class);
        return new JacksonJsonRedisSerializer<>(objectMapper, sportsListType);
    }

    @Bean("sessionSearchCacheValueSerializer")
    public RedisSerializer<PageResponse<TrainingSessionResponse>>
            sessionSearchCacheValueSerializer(ObjectMapper objectMapper) {
        JavaType pageType = objectMapper.getTypeFactory()
                .constructParametricType(PageResponse.class, TrainingSessionResponse.class);
        return new JacksonJsonRedisSerializer<>(objectMapper, pageType);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            @Qualifier("redisValueSerializer") RedisSerializer<Object> redisValueSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(redisValueSerializer);
        template.setHashValueSerializer(redisValueSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            @Qualifier("redisValueSerializer") RedisSerializer<Object> redisValueSerializer,
            @Qualifier("sportsCacheValueSerializer")
            RedisSerializer<List<SportResponse>> sportsCacheValueSerializer,
            @Qualifier("sessionSearchCacheValueSerializer")
            RedisSerializer<PageResponse<TrainingSessionResponse>>
                    sessionSearchCacheValueSerializer) {
        RedisCacheConfiguration defaultConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> RedisKeys.CACHE_PREFIX + cacheName + "::")
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(redisValueSerializer)
                );

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                CacheNames.SPORTS,
                defaultConfiguration
                        .entryTtl(Duration.ofHours(24))
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(
                                        sportsCacheValueSerializer
                                )
                        ),
                CacheNames.SESSION_SEARCH,
                defaultConfiguration
                        .entryTtl(Duration.ofMinutes(5))
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(
                                        sessionSearchCacheValueSerializer
                                )
                        )
        );

        RedisCacheWriter cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(
                connectionFactory,
                BatchStrategies.scan(1_000));

        return RedisCacheManager.builder(cacheWriter)
                .cacheDefaults(defaultConfiguration)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
