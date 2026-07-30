package com.csports.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.csports.location.Region;
import com.csports.location.RegionRepository;
import com.csports.sport.Sport;
import com.csports.sport.SportRepository;
import com.csports.user.Role;
import com.csports.user.User;
import com.csports.user.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SessionDiscoveryApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TrainingSessionRepository sessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SportRepository sportRepository;

    @Autowired
    private RegionRepository regionRepository;

    private TrainingSession matchingSession;
    private TrainingSession fullSession;
    private TrainingSession otherSession;
    private Sport matchingSport;
    private Region matchingRegion;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString();
        matchingRegion = saveRegion("Discovery Cairo " + unique, "Cairo");
        Region otherRegion = saveRegion("Discovery Giza " + unique, "Giza");
        matchingSport = sportRepository.saveAndFlush(Sport.builder()
                .name("Discovery Swimming " + unique)
                .build());
        Sport otherSport = sportRepository.saveAndFlush(Sport.builder()
                .name("Discovery Football " + unique)
                .build());
        User matchingTrainer = saveTrainer("Discovery Trainer " + unique, unique + "-one");
        User otherTrainer = saveTrainer("Other Trainer " + unique, unique + "-two");

        matchingSession = saveSession(
                matchingTrainer,
                matchingSport,
                matchingRegion,
                "Advanced swimming discovery " + unique,
                "Technique and endurance training",
                700.0,
                1,
                5,
                LocalDate.now().plusDays(20),
                LocalDate.now().plusDays(60));
        fullSession = saveSession(
                matchingTrainer,
                matchingSport,
                matchingRegion,
                "Full swimming discovery " + unique,
                "A full session that must be excluded",
                500.0,
                5,
                5,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(30));
        otherSession = saveSession(
                otherTrainer,
                otherSport,
                otherRegion,
                "Football discovery " + unique,
                "Different structured filters",
                300.0,
                0,
                8,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(8));
    }

    @Test
    void searchCombinesKeywordSportRegionPriceAndAvailabilityFilters() throws Exception {
        String path = "/api/v1/sessions"
                + "?q=advanced%20swimming"
                + "&sportId=" + matchingSport.getId()
                + "&regionId=" + matchingRegion.getId()
                + "&minPrice=600"
                + "&maxPrice=800"
                + "&availableOnly=true"
                + "&sortBy=price"
                + "&direction=desc"
                + "&page=0"
                + "&size=5";

        HttpResponse<String> response = get(path);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"id\":" + matchingSession.getId())
                .contains("\"trainerId\":" + matchingSession.getTrainer().getId())
                .contains("\"sportId\":" + matchingSport.getId())
                .contains("\"regionId\":" + matchingRegion.getId())
                .contains("\"regionName\":\"" + matchingRegion.getName() + "\"")
                .contains("\"remainingSeats\":4")
                .doesNotContain("\"id\":" + fullSession.getId())
                .doesNotContain("\"id\":" + otherSession.getId());
    }

    @Test
    void dateRangesUseSeriesOverlapAndSortResultsPredictably() throws Exception {
        LocalDate from = LocalDate.now().plusDays(25);
        LocalDate to = LocalDate.now().plusDays(35);
        String path = "/api/v1/sessions"
                + "?sportId=" + matchingSport.getId()
                + "&fromDate=" + from
                + "&toDate=" + to
                + "&sortBy=price"
                + "&direction=asc";

        HttpResponse<String> response = get(path);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"id\":" + matchingSession.getId())
                .contains("\"id\":" + fullSession.getId())
                .doesNotContain("\"id\":" + otherSession.getId());
        assertThat(response.body().indexOf("\"id\":" + fullSession.getId()))
                .isLessThan(response.body().indexOf("\"id\":" + matchingSession.getId()));
    }

    @Test
    void invalidSearchRangesUseTheConsistentApiError() throws Exception {
        HttpResponse<String> response = get(
                "/api/v1/sessions?minPrice=900&maxPrice=100");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains(
                "\"code\":\"BUSINESS_RULE_VIOLATION\"",
                "Maximum price cannot be lower");
    }

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private Region saveRegion(String name, String city) {
        return regionRepository.saveAndFlush(Region.builder()
                .country("Egypt")
                .city(city)
                .name(name)
                .latitude(30.05)
                .longitude(31.25)
                .build());
    }

    private User saveTrainer(String name, String unique) {
        return userRepository.saveAndFlush(User.builder()
                .name(name)
                .email(unique + "@csports.test")
                .phoneNumber("+201" + Math.abs(unique.hashCode()) + "88")
                .password("not-used")
                .age(30)
                .role(Role.TRAINER)
                .build());
    }

    private TrainingSession saveSession(
            User trainer,
            Sport sport,
            Region region,
            String title,
            String description,
            double price,
            int currentParticipants,
            int maxParticipants,
            LocalDate startDate,
            LocalDate endDate) {
        DayOfWeek day = startDate.getDayOfWeek();
        return sessionRepository.saveAndFlush(TrainingSession.builder()
                .trainer(trainer)
                .sport(sport)
                .region(region)
                .title(title)
                .description(description)
                .locationName(region.getName())
                .latitude(region.getLatitude())
                .longitude(region.getLongitude())
                .startDate(startDate)
                .endDate(endDate)
                .startTime(LocalTime.of(18, 0))
                .durationMinutes(90)
                .days(Set.of(day))
                .maxParticipants(maxParticipants)
                .currentParticipants(currentParticipants)
                .price(price)
                .build());
    }
}
