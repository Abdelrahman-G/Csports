package com.csports.infrastructure.redis;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@ConditionalOnProperty(
        name = "csports.cache.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class CacheConfig {
}
