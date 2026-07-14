package com.Csports.Csports.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {

                RedisTemplate<String, Object> template = new RedisTemplate<>();

                template.setConnectionFactory(connectionFactory);

                template.setKeySerializer(new StringRedisSerializer());

                template.setHashKeySerializer(new StringRedisSerializer());

                JacksonJsonRedisSerializer<Object> serializer = new JacksonJsonRedisSerializer<>(Object.class);

                template.setValueSerializer(serializer);

                template.setHashValueSerializer(serializer);

                template.afterPropertiesSet();

                return template;
        }
}