package com.csports.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("migration-test")
class FlywayMigrationTest {

    private static final Pattern ACCESS_TOKEN =
            Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void initialMigrationBuildsSchemaThatHibernateCanValidate() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history "
                        + "where version = '1' and success = true",
                Integer.class
        );

        assertThat(migrationCount).isEqualTo(1);
    }

    @Test
    void loginSportsListAndGeneratedDocumentationWorkOverHttp() throws Exception {
        jdbcTemplate.update(
                """
                insert into users
                    (name, phone_number, email, password, age, role)
                values (?, ?, ?, ?, ?, ?)
                """,
                "Smoke Test User",
                "+201000000099",
                "smoke-test@csports.local",
                passwordEncoder.encode("SmokePassword123!"),
                25,
                "USER"
        );

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> openApiResponse = client.send(
                HttpRequest.newBuilder(uri("/v3/api-docs")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> loginResponse = client.send(
                HttpRequest.newBuilder(uri("/api/v1/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                """
                                {
                                  "identifier": "smoke-test@csports.local",
                                  "password": "SmokePassword123!"
                                }
                                """
                        ))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        var tokenMatch = ACCESS_TOKEN.matcher(loginResponse.body());
        assertThat(openApiResponse.statusCode()).isEqualTo(200);
        assertThat(openApiResponse.body()).contains(
                "\"/api/v1/auth/login\"",
                "\"/api/v1/sports/list\"",
                "\"/sports/list\""
        );
        assertThat(loginResponse.statusCode()).isEqualTo(200);
        assertThat(tokenMatch.find()).isTrue();

        HttpResponse<String> sportsResponse = client.send(
                HttpRequest.newBuilder(uri("/api/v1/sports/list"))
                        .header("Authorization", "Bearer " + tokenMatch.group(1))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(sportsResponse.statusCode()).isEqualTo(200);
        assertThat(sportsResponse.body()).isEqualTo("[]");
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
