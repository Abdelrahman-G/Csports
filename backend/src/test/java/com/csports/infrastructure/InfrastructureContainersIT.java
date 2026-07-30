package com.csports.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.csports.common.pagination.PageResponse;
import com.csports.infrastructure.redis.CacheNames;
import com.csports.session.dto.TrainingSessionResponse;

import java.util.List;

@SpringBootTest
@ActiveProfiles("integration")
@Testcontainers(disabledWithoutDocker = true)
class InfrastructureContainersIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7.4"))
                    .withExposedPorts(6379);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void flywayAndRedisAreAvailableWithRealInfrastructure() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true",
                Integer.class
        );
        String redisResponse;
        try (var connection = redisConnectionFactory.getConnection()) {
            redisResponse = connection.ping();
        }

        assertThat(migrationCount).isPositive();
        assertThat(redisResponse).isEqualToIgnoringCase("PONG");
    }

    @Test
    void sessionSearchCacheRoundTripsThroughRealRedis() {
        Cache cache = cacheManager.getCache(CacheNames.SESSION_SEARCH);
        assertThat(cache).isNotNull();
        PageResponse<TrainingSessionResponse> expected = new PageResponse<>(
                List.of(),
                0,
                10,
                0,
                0,
                true,
                true);

        cache.put("integration-test", expected);
        PageResponse<?> actual = cache.get("integration-test", PageResponse.class);

        assertThat(actual).isEqualTo(expected);
        cache.evict("integration-test");
    }
}
