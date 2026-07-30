package com.csports.session;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.csports.session.dto.SessionSearchRequest;

import jakarta.persistence.criteria.Predicate;

final class TrainingSessionSpecifications {

    private TrainingSessionSpecifications() {
    }

    static Specification<TrainingSession> matches(SessionSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(
                    root.get("status"),
                    TrainingSessionStatus.SCHEDULED));

            // Date filters use interval overlap. A session series is discoverable
            // when any part of it overlaps the requested period.
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("endDate"),
                    request.effectiveFromDate()));
            if (request.toDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("startDate"),
                        request.toDate()));
            }

            if (request.sportId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("sport").get("id"),
                        request.sportId()));
            }
            if (request.trainerId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("trainer").get("id"),
                        request.trainerId()));
            }
            if (request.regionId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("region").get("id"),
                        request.regionId()));
            }
            if (request.minPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("price"),
                        request.minPrice()));
            }
            if (request.maxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("price"),
                        request.maxPrice()));
            }
            if (request.availableOnly()) {
                predicates.add(criteriaBuilder.lessThan(
                        root.get("currentParticipants"),
                        root.get("maxParticipants")));
            }
            if (request.q() != null) {
                String pattern = "%" + escapeLike(request.q()) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("title")),
                                pattern,
                                '\\'),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("description")),
                                pattern,
                                '\\'),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("locationName")),
                                pattern,
                                '\\')));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
