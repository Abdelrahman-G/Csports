package com.csports.infrastructure.redis;

public final class RedisKeys {

    public static final String APPLICATION_PREFIX = "csports:v1:";
    public static final String CACHE_PREFIX = APPLICATION_PREFIX + "cache:";
    public static final String TOKEN_BLACKLIST_PREFIX = APPLICATION_PREFIX + "auth:blacklist:";

    private RedisKeys() {
    }
}
