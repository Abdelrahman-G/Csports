package com.csports.notification;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.csports.security.JwtService;
import com.csports.user.Role;
import com.csports.user.User;
import com.csports.user.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NotificationApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserNotificationRepository notificationRepository;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    private User user;
    private User otherUser;
    private String userToken;
    private String trainerToken;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString();
        user = saveUser("Notification User", "user-" + unique, Role.USER);
        otherUser = saveUser("Other User", "other-" + unique, Role.USER);
        User trainer = saveUser("Notification Trainer", "trainer-" + unique, Role.TRAINER);
        userToken = jwtService.generateAccessToken(user);
        trainerToken = jwtService.generateAccessToken(trainer);
    }

    @Test
    void participantCanListCountAndReadOnlyTheirNotifications() throws Exception {
        UserNotification first = saveNotification(user, 10L, "Location changed");
        saveNotification(user, 11L, "Session cancelled");
        UserNotification someoneElses = saveNotification(otherUser, 12L, "Private update");

        HttpResponse<String> countBefore = request(
                "GET", "/api/v1/notifications/unread-count", userToken);
        assertThat(countBefore.statusCode()).isEqualTo(200);
        assertThat(countBefore.body()).contains("\"unreadCount\":2");

        HttpResponse<String> list = request(
                "GET", "/api/v1/notifications?page=0&size=10", userToken);
        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(list.body())
                .contains("Location changed", "Session cancelled", "\"totalElements\":2")
                .doesNotContain("Private update");

        HttpResponse<String> markedRead = request(
                "PATCH", "/api/v1/notifications/" + first.getId() + "/read", userToken);
        assertThat(markedRead.statusCode()).isEqualTo(200);
        assertThat(markedRead.body()).contains("\"read\":true");

        HttpResponse<String> countAfter = request(
                "GET", "/api/v1/notifications/unread-count", userToken);
        assertThat(countAfter.body()).contains("\"unreadCount\":1");

        HttpResponse<String> otherUsersNotification = request(
                "PATCH",
                "/api/v1/notifications/" + someoneElses.getId() + "/read",
                userToken);
        assertThat(otherUsersNotification.statusCode()).isEqualTo(404);
    }

    @Test
    void trainerCannotUseParticipantNotificationEndpoints() throws Exception {
        HttpResponse<String> response = request(
                "GET", "/api/v1/notifications/unread-count", trainerToken);

        assertThat(response.statusCode()).isEqualTo(403);
    }

    private UserNotification saveNotification(User recipient, Long sessionId, String title) {
        return notificationRepository.saveAndFlush(UserNotification.builder()
                .recipient(recipient)
                .sessionId(sessionId)
                .type(NotificationType.SESSION_UPDATED)
                .title(title)
                .message("A booked session changed.")
                .build());
    }

    private HttpResponse<String> request(String method, String path, String token)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + token);

        if ("PATCH".equals(method)) {
            builder.method("PATCH", HttpRequest.BodyPublishers.noBody());
        } else {
            builder.GET();
        }

        return HttpClient.newHttpClient().send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private User saveUser(String name, String unique, Role role) {
        return userRepository.saveAndFlush(User.builder()
                .name(name)
                .email(unique + "@csports.test")
                .phoneNumber("+201" + Math.abs(unique.hashCode()) + "55")
                .password("not-used")
                .age(30)
                .role(role)
                .build());
    }
}
