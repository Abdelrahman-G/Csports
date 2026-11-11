package com.csports.booking;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.csports.booking.dto.BookingSearchRequest;
import com.csports.course.TrainingCourseStatus;

import jakarta.persistence.criteria.Predicate;

final class BookingSpecifications {

    private BookingSpecifications() {
    }

    static Specification<Booking> forUser(
            Long userId,
            BookingSearchRequest request,
            LocalDate today) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));

            if (request.status() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), request.status()));
            }
            if (request.courseId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("course").get("id"),
                        request.courseId()));
            }

            switch (request.view()) {
                case UPCOMING -> {
                    predicates.add(criteriaBuilder.equal(
                            root.get("status"),
                            BookingStatus.CONFIRMED));
                    predicates.add(criteriaBuilder.equal(
                            root.get("course").get("status"),
                            TrainingCourseStatus.SCHEDULED));
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                            root.get("course").get("endDate"),
                            today));
                }
                case HISTORY -> predicates.add(criteriaBuilder.or(
                        criteriaBuilder.notEqual(
                                root.get("status"),
                                BookingStatus.CONFIRMED),
                        criteriaBuilder.notEqual(
                                root.get("course").get("status"),
                                TrainingCourseStatus.SCHEDULED),
                        criteriaBuilder.lessThan(
                                root.get("course").get("endDate"),
                                today)));
                case ALL -> {
                    // No additional lifecycle predicate.
                }
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
