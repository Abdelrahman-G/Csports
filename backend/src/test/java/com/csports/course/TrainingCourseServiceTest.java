package com.csports.course;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.csports.common.pagination.PageResponse;
import com.csports.location.Region;
import com.csports.location.RegionRepository;
import com.csports.course.dto.TrainingCourseResponse;
import com.csports.sport.Sport;
import com.csports.sport.SportRepository;
import com.csports.user.Role;
import com.csports.user.User;
import com.csports.user.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class TrainingCourseServiceTest {

    @Autowired
    private TrainingCourseService trainingCourseService;

    @Autowired
    private TrainingCourseRepository trainingCourseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SportRepository sportRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Test
    void upcomingCoursesCanMapLazyTrainerAndSportAssociations() {
        String uniqueValue = UUID.randomUUID().toString();

        User trainer = userRepository.saveAndFlush(User.builder()
                .name("Course Test Trainer")
                .phoneNumber("+20" + uniqueValue)
                .email(uniqueValue + "@csports.test")
                .password("not-used")
                .age(30)
                .role(Role.TRAINER)
                .build());

        Sport sport = sportRepository.saveAndFlush(Sport.builder()
                .name("Test Sport " + uniqueValue)
                .build());
        Region region = regionRepository.saveAndFlush(Region.builder()
                .country("Egypt")
                .city("Cairo")
                .name("Test Region " + uniqueValue)
                .latitude(30.0444)
                .longitude(31.2357)
                .build());
        LocalDate startDate = LocalDate.now().plusDays(1);

        TrainingCourse savedCourse = trainingCourseRepository.saveAndFlush(
                TrainingCourse.builder()
                        .trainer(trainer)
                        .sport(sport)
                        .region(region)
                        .title("Lazy association test")
                        .description("Regression test for course response mapping")
                        .locationName("Test location")
                        .latitude(30.0444)
                        .longitude(31.2357)
                        .startDate(startDate)
                        .endDate(startDate.plusDays(1))
                        .startTime(LocalTime.of(18, 0))
                        .durationMinutes(60)
                        .days(Set.of(startDate.getDayOfWeek()))
                        .maxParticipants(10)
                        .price(100.0)
                        .build());

        PageResponse<TrainingCourseResponse> response =
                trainingCourseService.getAllUpcomingCourses(0, 20);

        TrainingCourseResponse mappedCourse = response.content().stream()
                .filter(item -> item.id().equals(savedCourse.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(mappedCourse.trainerName()).isEqualTo(trainer.getName());
        assertThat(mappedCourse.sport()).isEqualTo(sport.getName());
        assertThat(mappedCourse.days()).containsExactly(startDate.getDayOfWeek());
    }
}
