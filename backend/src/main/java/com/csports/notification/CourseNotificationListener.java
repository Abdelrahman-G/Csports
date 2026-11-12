package com.csports.notification;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.csports.announcement.CourseAnnouncementService;

@Component
public class CourseNotificationListener {

    private final CourseAnnouncementService announcements;

    public CourseNotificationListener(CourseAnnouncementService announcements) {
        this.announcements = announcements;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void recordCourseNotice(CourseChangedEvent event) {
        announcements.recordCourseChange(event);
    }
}
