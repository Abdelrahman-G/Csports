package com.csports.announcement;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CourseAnnouncementRepository extends JpaRepository<CourseAnnouncement, Long> {
    String RESPONSE = """
            select new com.csports.announcement.AnnouncementResponse(
                a.id, a.course.id, a.author.id, a.author.name, a.type, a.title, a.content, a.createdAt)
            from CourseAnnouncement a
            """;

    @Query(value = RESPONSE + " where a.course.id = :courseId",
            countQuery = "select count(a) from CourseAnnouncement a where a.course.id = :courseId")
    Page<AnnouncementResponse> findResponsesByCourseId(Long courseId, Pageable pageable);

    @Query(RESPONSE + " where a.course.id = :courseId and a.id = :announcementId")
    Optional<AnnouncementResponse> findResponse(Long courseId, Long announcementId);
}
