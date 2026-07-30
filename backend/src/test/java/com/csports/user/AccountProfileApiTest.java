package com.csports.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.csports.auth.exception.EmailAlreadyExistsException;
import com.csports.location.Region;
import com.csports.location.RegionRepository;
import com.csports.location.UserLocation;
import com.csports.security.JwtService;
import com.csports.sport.Sport;
import com.csports.sport.SportRepository;
import com.csports.trainer.TrainerProfile;
import com.csports.trainer.TrainerProfileRepository;
import com.csports.user.dto.UpdateUserProfileRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AccountProfileApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private SportRepository sportRepository;

    @Autowired
    private TrainerProfileRepository trainerProfileRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    private User user;
    private User trainer;
    private Region secondRegion;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        String unique = UUID.randomUUID().toString();
        Region firstRegion = regionRepository.saveAndFlush(Region.builder()
                .country("Egypt")
                .city("Cairo")
                .name("Profile Region A " + unique)
                .latitude(30.0581)
                .longitude(31.3302)
                .build());
        secondRegion = regionRepository.saveAndFlush(Region.builder()
                .country("Egypt")
                .city("Cairo")
                .name("Profile Region B " + unique)
                .latitude(30.0285)
                .longitude(31.4913)
                .build());
        Sport sport = sportRepository.saveAndFlush(Sport.builder()
                .name("Profile Sport " + unique)
                .build());

        user = saveUser("Profile User", "user-" + unique, Role.USER, firstRegion, 25);
        trainer = saveUser("Profile Trainer", "trainer-" + unique, Role.TRAINER, firstRegion, 30);
        trainerProfileRepository.saveAndFlush(TrainerProfile.builder()
                .user(trainer)
                .sport(sport)
                .bio("Original trainer bio")
                .experienceYears(5)
                .build());
    }

    @Test
    void currentAccountCanBeReadAndPartiallyUpdatedOverHttp() throws Exception {
        String token = jwtService.generateAccessToken(user);
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> currentResponse = client.send(
                authenticatedRequest("/api/v1/users/me", token).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> updateResponse = client.send(
                authenticatedRequest("/api/v1/users/me", token)
                        .header("Content-Type", "application/json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(
                                """
                                {
                                  "name": "Updated Profile User",
                                  "regionId": %d,
                                  "latitude": 30.0285,
                                  "longitude": 31.4913
                                }
                                """.formatted(secondRegion.getId())))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> legacyResponse = client.send(
                authenticatedRequest("/api/v1/auth/me", token).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(currentResponse.statusCode()).isEqualTo(200);
        assertThat(currentResponse.body())
                .contains("\"id\":" + user.getId())
                .contains("\"email\":\"" + user.getEmail() + "\"")
                .doesNotContain("password");

        assertThat(updateResponse.statusCode()).isEqualTo(200);
        assertThat(updateResponse.body())
                .contains("\"id\":" + user.getId())
                .contains("\"name\":\"Updated Profile User\"")
                .contains("\"email\":\"" + user.getEmail() + "\"")
                .contains("\"regionId\":" + secondRegion.getId());

        assertThat(legacyResponse.statusCode()).isEqualTo(200);
        assertThat(legacyResponse.body())
                .contains("\"name\":\"Updated Profile User\"")
                .doesNotContain("password");
    }

    @Test
    void trainerProfileHasPublicAndOwnerSpecificOperations() throws Exception {
        String trainerToken = jwtService.generateAccessToken(trainer);
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> publicResponse = client.send(
                HttpRequest.newBuilder(uri("/api/v1/trainers/" + trainer.getId()))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> privateWithoutToken = client.send(
                HttpRequest.newBuilder(uri("/api/v1/trainers/me")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> updateResponse = client.send(
                authenticatedRequest("/api/v1/trainers/me", trainerToken)
                        .header("Content-Type", "application/json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(
                                """
                                {
                                  "bio": "Updated trainer bio",
                                  "experienceYears": 6
                                }
                                """))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(publicResponse.statusCode()).isEqualTo(200);
        assertThat(publicResponse.body())
                .contains("\"id\":" + trainer.getId())
                .contains("\"bio\":\"Original trainer bio\"")
                .doesNotContain(trainer.getEmail())
                .doesNotContain(trainer.getPhoneNumber());

        assertThat(privateWithoutToken.statusCode()).isEqualTo(401);
        assertThat(updateResponse.statusCode()).isEqualTo(200);
        assertThat(updateResponse.body())
                .contains("\"id\":" + trainer.getId())
                .contains("\"bio\":\"Updated trainer bio\"")
                .contains("\"experienceYears\":6");
    }

    @Test
    void accountUpdateRejectsAnotherUsersEmail() {
        authenticate(user);

        assertThatThrownBy(() -> userService.updateMyProfile(
                new UpdateUserProfileRequest(
                        null,
                        trainer.getEmail(),
                        null,
                        null,
                        null,
                        null,
                        null)))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    private User saveUser(
            String name,
            String unique,
            Role role,
            Region region,
            int age) {
        User account = User.builder()
                .name(name)
                .email(unique + "@csports.test")
                .phoneNumber("+201" + Math.abs(unique.hashCode()) + "77")
                .password("not-used")
                .age(age)
                .role(role)
                .build();
        account.setLocation(UserLocation.builder()
                .region(region)
                .latitude(region.getLatitude())
                .longitude(region.getLongitude())
                .build());
        return userRepository.saveAndFlush(account);
    }

    private void authenticate(User account) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        account,
                        null,
                        account.getAuthorities()));
    }

    private HttpRequest.Builder authenticatedRequest(String path, String token) {
        return HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + token);
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
