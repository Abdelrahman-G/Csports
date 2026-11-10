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
                        + "where version in ('1', '2', '3', '4', '5', '6', '9') and success = true",
                Integer.class
        );

        assertThat(migrationCount).isEqualTo(7);

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

        Integer courseRegionColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where table_name = 'training_course' and column_name = 'region_id'",
                Integer.class
        );
        assertThat(courseRegionColumnCount).isEqualTo(1);

        Integer discoveryIndexCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.indexes "
                        + "where upper(index_name) in ("
                        + "'IDX_TRAINING_COURSE_STATUS_END_START', "
                        + "'IDX_TRAINING_COURSE_SPORT_STATUS_START', "
                        + "'IDX_TRAINING_COURSE_REGION_STATUS_START', "
                        + "'IDX_TRAINING_COURSE_TRAINER_START', "
                        + "'IDX_TRAINING_COURSE_STATUS_PRICE')",
                Integer.class
        );
        assertThat(discoveryIndexCount).isEqualTo(5);

        Integer bookingLifecycleColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where table_name = 'booking' "
                        + "and column_name in ('cancelled_at', 'active_marker')",
                Integer.class
        );
        assertThat(bookingLifecycleColumnCount).isEqualTo(2);

        Integer bookingIntegrityIndexCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.indexes "
                        + "where upper(index_name) in ("
                        + "'UQ_BOOKING_CONFIRMED_USER_COURSE', "
                        + "'IDX_BOOKING_USER_STATUS_BOOKED_AT')",
                Integer.class
        );
        assertThat(bookingIntegrityIndexCount).isEqualTo(2);
    }

    @Test
    void courseRenamePreservesExistingBookingsAndNotificationHistory() {
        String url = "jdbc:h2:mem:rename-upgrade;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=DAY";
        var source = new org.springframework.jdbc.datasource.SingleConnectionDataSource(url, "sa", "", true);
        org.flywaydb.core.Flyway.configure().dataSource(source)
                .locations("classpath:db/migration").target("6").load().migrate();
        var database = new JdbcTemplate(source);
        database.update("insert into users (id,name,phone_number,email,password,age,role) values (900,'Trainer','900','rename@test','unused',30,'TRAINER')");
        database.update("""
                insert into training_session (id,trainer_id,sport_id,region_id,title,version,location_name,
                    latitude,longitude,start_date,end_date,start_time,duration_minutes,max_participants,current_participants,price,created_at)
                values (900,900,(select min(id) from sport),(select min(id) from region),'Preserved course',0,'Club',30,31,
                    '2030-01-01','2030-02-01','18:00',60,10,1,100,current_timestamp)
                """);
        database.update("insert into training_session_days (session_id,day) values (900,'TUESDAY')");
        database.update("insert into booking (user_id,session_id,booked_at,version,status) values (900,900,current_timestamp,0,'CONFIRMED')");
        database.update("""
                insert into user_notification (recipient_id,session_id,type,title,message,created_at)
                values (900,900,'SESSION_UPDATED','Old title','Old message',current_timestamp)
                """);
        org.flywaydb.core.Flyway.configure().dataSource(source)
                .locations("classpath:db/migration").load().migrate();
        assertThat(database.queryForObject("select title from training_course where id=900", String.class)).isEqualTo("Preserved course");
        assertThat(database.queryForObject("select course_id from booking where user_id=900", Long.class)).isEqualTo(900L);
        assertThat(database.queryForObject("select day from training_course_days where course_id=900", String.class)).isEqualTo("TUESDAY");
        assertThat(database.queryForMap("select course_id,type,title,message from user_notification where recipient_id=900"))
                .containsEntry("course_id", 900L).containsEntry("type", "COURSE_UPDATED")
                .containsEntry("title", "Old title").containsEntry("message", "Old message");
        source.destroy();
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
        assertThat(sportsResponse.body()).contains(
                "\"name\":\"Swimming\"",
                "\"name\":\"Football\"",
                "\"name\":\"Basketball\""
        );

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
                HttpRequest.newBuilder(uri("/api/v1/courses"))
                        .header("Authorization", "Bearer " + tokenMatch.group(1))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                """
                                {
                                  "title": "User cannot create this",
                                  "locationName": "Nasr City",
                                  "regionId": 1,
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
                                  "age": 10
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
                "\"email\""
        );
    }

    @Test
    void userAndTrainerSignupDoNotRequireAHomeLocation() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> userResponse = client.send(
                HttpRequest.newBuilder(uri("/api/v1/auth/register/user"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                """
                                {
                                  "name": "Location Free User",
                                  "email": "location.free.user@gmail.com",
                                  "phoneNumber": "01100000011",
                                  "password": "StrongPassword123!",
                                  "age": 24
                                }
                                """))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> trainerResponse = client.send(
                HttpRequest.newBuilder(uri("/api/v1/auth/register/trainer"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                """
                                {
                                  "name": "Location Free Trainer",
                                  "email": "location.free.trainer@gmail.com",
                                  "phoneNumber": "01200000011",
                                  "password": "StrongPassword123!",
                                  "age": 30,
                                  "bio": "Patient swimming coach.",
                                  "experienceYears": 5,
                                  "sportId": 1
                                }
                                """))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        Integer savedLocations = jdbcTemplate.queryForObject(
                """
                select count(*)
                from user_location location
                join users account on account.id = location.user_id
                where account.email in (
                    'location.free.user@gmail.com',
                    'location.free.trainer@gmail.com'
                )
                """,
                Integer.class);

        assertThat(userResponse.statusCode()).isEqualTo(200);
        assertThat(trainerResponse.statusCode()).isEqualTo(200);
        assertThat(savedLocations).isZero();
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
