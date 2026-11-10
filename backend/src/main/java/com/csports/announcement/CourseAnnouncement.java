package com.csports.announcement;

import java.time.Instant;
import com.csports.course.TrainingCourse;
import com.csports.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "course_announcement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseAnnouncement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, updatable = false)
    private TrainingCourse course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false, updatable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32, updatable = false)
    private AnnouncementType type;

    @Column(nullable = false, length = 150, updatable = false)
    private String title;

    @Column(nullable = false, length = 2000, updatable = false)
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public CourseAnnouncement(TrainingCourse course, User author, AnnouncementType type,
            String title, String content, Instant createdAt) {
        this.course = course;
        this.author = author;
        this.type = type;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
    }
}
