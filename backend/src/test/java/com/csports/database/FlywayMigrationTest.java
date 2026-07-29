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
    void allMigrationsBuildSchemaThatHibernateCanValidate() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history "
                        + "where version in ('1', '2', '3') and success = true",
                Integer.class
        );

        assertThat(migrationCount).isEqualTo(3);

        Integer notificationTableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables "
                        + "where table_name = 'user_notification'",
                Integer.class
        );
        assertThat(notificationTableCount).isEqualTo(1);

        Integer bookingStatusColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where table_name = 'booking' and column_name = 'status'",
                Integer.class
        );
        assertThat(bookingStatusColumnCount).isEqualTo(1);
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
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(sportsResponse.statusCode()).isEqualTo(200);
        assertThat(sportsResponse.body()).isEqualTo("[]");

        HttpResponse<String> unauthenticatedResponse = client.send(
                HttpRequest.newBuilder(uri("/api/v1/bookings/me")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(unauthenticatedResponse.statusCode()).isEqualTo(401);
        assertThat(unauthenticatedResponse.body()).contains(
                "\"code\":\"AUTHENTICATION_REQUIRED\"",
                "\"path\":\"/api/v1/bookings/me\""
        );

        HttpResponse<String> invalidTokenResponse = client.send(
                HttpRequest.newBuilder(uri("/api/v1/bookings/me"))
                        .header("Authorization", "Bearer this-is-not-a-jwt")
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(invalidTokenResponse.statusCode()).isEqualTo(401);
        assertThat(invalidTokenResponse.body()).contains("\"code\":\"AUTHENTICATION_REQUIRED\"");

        HttpResponse<String> forbiddenResponse = client.send(
                HttpRequest.newBuilder(uri("/api/v1/sessions"))
                        .header("Authorization", "Bearer " + tokenMatch.group(1))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                """
                                {
                                  "title": "User cannot create this",
                                  "locationName": "Nasr City",
                                  "latitude": 30.0581,
                                  "longitude": 31.3302,
                                  "startDate": "2099-01-05",
                                  "endDate": "2099-01-05",
                                  "startTime": "18:00:00",
                                  "durationMinutes": 60,
                                  "days": ["MONDAY"],
                                  "maxParticipants": 10,
                                  "price": 100
                                }
                                """
                        ))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(forbiddenResponse.statusCode()).isEqualTo(403);
        assertThat(forbiddenResponse.body()).contains("\"code\":\"ACCESS_DENIED\"");

        HttpResponse<String> validationResponse = client.send(
                HttpRequest.newBuilder(uri("/api/v1/auth/register/user"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                """
                                {
                                  "name": "",
                                  "email": "invalid-email",
                                  "phoneNumber": "12",
                                  "password": "short",
                                  "age": 10,
                                  "regionId": 1,
                                  "latitude": 28.0,
                                  "longitude": 32.0
                                }
                                """
                        ))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(validationResponse.statusCode()).isEqualTo(400);
        assertThat(validationResponse.body()).contains(
                "\"code\":\"VALIDATION_FAILED\"",
                "\"fieldErrors\"",
                "\"latitude\"",
                "\"longitude\"",
                "\"email\""
        );
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
