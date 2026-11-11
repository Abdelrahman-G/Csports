package com.csports.course;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csports.common.pagination.PageResponse;
import com.csports.infrastructure.redis.CacheNames;
import com.csports.course.dto.CourseSearchRequest;
import com.csports.course.dto.TrainingCourseResponse;

@Service
public class TrainingCourseSearchService {

    private final TrainingCourseRepository trainingCourseRepository;
    private final TrainingCourseMapper trainingCourseMapper;
    private final NearbyCourseSearchRepository nearbyCourseSearchRepository;

    public TrainingCourseSearchService(
            TrainingCourseRepository trainingCourseRepository,
            TrainingCourseMapper trainingCourseMapper,
            NearbyCourseSearchRepository nearbyCourseSearchRepository) {
        this.trainingCourseRepository = trainingCourseRepository;
        this.trainingCourseMapper = trainingCourseMapper;
        this.nearbyCourseSearchRepository = nearbyCourseSearchRepository;
    }

    @Cacheable(
            cacheNames = CacheNames.COURSE_SEARCH,
            key = "#request.cacheKey()",
            condition = "!#request.nearby()",
            sync = true)
    @Transactional(readOnly = true)
    public PageResponse<TrainingCourseResponse> search(CourseSearchRequest request) {
        request.validateRanges();

        if (request.nearby()) {
            return searchNearby(request);
        }

        Sort.Direction direction = Sort.Direction.fromString(request.direction());
        Sort sort = Sort.by(direction, propertyFor(request.sortBy()));
        if ("startDate".equalsIgnoreCase(request.sortBy())) {
            sort = sort.and(Sort.by(direction, "startTime"));
        }
        sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));

        PageRequest pageable = PageRequest.of(request.page(), request.size(), sort);
        Page<TrainingCourseResponse> result = trainingCourseRepository
                .findAll(TrainingCourseSpecifications.matches(request), pageable)
                .map(trainingCourseMapper::toResponse);

        return new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast());
    }

    private PageResponse<TrainingCourseResponse> searchNearby(
            CourseSearchRequest request) {
        NearbyCoursePage nearbyPage = nearbyCourseSearchRepository.findNearby(request);
        if (nearbyPage.matches().isEmpty()) {
            return pageResponse(request, nearbyPage.totalElements(), java.util.List.of());
        }

        java.util.List<Long> orderedIds = nearbyPage.matches().stream()
                .map(NearbyCourseMatch::courseId)
                .toList();
        Map<Long, TrainingCourse> coursesById = trainingCourseRepository
                .findAllWithDetailsByIdIn(orderedIds)
                .stream()
                .collect(Collectors.toMap(TrainingCourse::getId, Function.identity()));
        Map<Long, Double> distanceById = nearbyPage.matches().stream()
                .collect(Collectors.toMap(
                        NearbyCourseMatch::courseId,
                        NearbyCourseMatch::distanceMeters));

        java.util.List<TrainingCourseResponse> content = orderedIds.stream()
                .map(coursesById::get)
                .filter(java.util.Objects::nonNull)
                .map(course -> trainingCourseMapper.toResponse(
                        course,
                        distanceById.get(course.getId())))
                .toList();

        return pageResponse(request, nearbyPage.totalElements(), content);
    }

    private PageResponse<TrainingCourseResponse> pageResponse(
            CourseSearchRequest request,
            long totalElements,
            java.util.List<TrainingCourseResponse> content) {
        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / request.size());
        return new PageResponse<>(
                content,
                request.page(),
                request.size(),
                totalElements,
                totalPages,
                request.page() == 0,
                totalPages == 0 || request.page() >= totalPages - 1);
    }

    private String propertyFor(String requestedSort) {
        return switch (requestedSort.toLowerCase()) {
            case "price" -> "price";
            case "createdat" -> "createdAt";
            default -> "startDate";
        };
    }
}
