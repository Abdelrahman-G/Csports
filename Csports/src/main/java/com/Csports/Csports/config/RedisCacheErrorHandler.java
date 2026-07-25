package com.Csports.Csports.config;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;

@Component
public class RedisCacheErrorHandler implements CacheErrorHandler {

    private static final Logger logger = Logger.getLogger(RedisCacheErrorHandler.class.getName());

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        if (exception instanceof SerializationException) {
            logger.warning("Cache read error for key " + key + ": " + exception.getMessage());
            return;
        }
        throw exception;
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        logger.warning("Cache write error for key " + key + ": " + exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        logger.warning("Cache evict error for key " + key + ": " + exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        logger.warning("Cache clear error: " + exception.getMessage());
    }
}
