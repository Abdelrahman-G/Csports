package com.csports.session;

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
import com.csports.session.dto.SessionSearchRequest;
import com.csports.session.dto.TrainingSessionResponse;

@Service
public class TrainingSessionSearchService {

    private final TrainingSessionRepository trainingSessionRepository;
    private final TrainingSessionMapper trainingSessionMapper;
    private final NearbySessionSearchRepository nearbySessionSearchRepository;

    public TrainingSessionSearchService(
            TrainingSessionRepository trainingSessionRepository,
            TrainingSessionMapper trainingSessionMapper,
            NearbySessionSearchRepository nearbySessionSearchRepository) {
        this.trainingSessionRepository = trainingSessionRepository;
        this.trainingSessionMapper = trainingSessionMapper;
        this.nearbySessionSearchRepository = nearbySessionSearchRepository;
    }

    @Cacheable(
            cacheNames = CacheNames.SESSION_SEARCH,
            key = "#request.cacheKey()",
            condition = "!#request.nearby()",
            sync = true)
    @Transactional(readOnly = true)
    public PageResponse<TrainingSessionResponse> search(SessionSearchRequest request) {
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
        Page<TrainingSessionResponse> result = trainingSessionRepository
                .findAll(TrainingSessionSpecifications.matches(request), pageable)
                .map(trainingSessionMapper::toResponse);

        return new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast());
    }

    private PageResponse<TrainingSessionResponse> searchNearby(
            SessionSearchRequest request) {
        NearbySessionPage nearbyPage = nearbySessionSearchRepository.findNearby(request);
        if (nearbyPage.matches().isEmpty()) {
            return pageResponse(request, nearbyPage.totalElements(), java.util.List.of());
        }

        java.util.List<Long> orderedIds = nearbyPage.matches().stream()
                .map(NearbySessionMatch::sessionId)
                .toList();
        Map<Long, TrainingSession> sessionsById = trainingSessionRepository
                .findAllWithDetailsByIdIn(orderedIds)
                .stream()
                .collect(Collectors.toMap(TrainingSession::getId, Function.identity()));
        Map<Long, Double> distanceById = nearbyPage.matches().stream()
                .collect(Collectors.toMap(
                        NearbySessionMatch::sessionId,
                        NearbySessionMatch::distanceMeters));

        java.util.List<TrainingSessionResponse> content = orderedIds.stream()
                .map(sessionsById::get)
                .filter(java.util.Objects::nonNull)
                .map(session -> trainingSessionMapper.toResponse(
                        session,
                        distanceById.get(session.getId())))
                .toList();

        return pageResponse(request, nearbyPage.totalElements(), content);
    }

    private PageResponse<TrainingSessionResponse> pageResponse(
            SessionSearchRequest request,
            long totalElements,
            java.util.List<TrainingSessionResponse> content) {
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
