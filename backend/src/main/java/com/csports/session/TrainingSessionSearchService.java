package com.csports.session;

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

    public TrainingSessionSearchService(
            TrainingSessionRepository trainingSessionRepository,
            TrainingSessionMapper trainingSessionMapper) {
        this.trainingSessionRepository = trainingSessionRepository;
        this.trainingSessionMapper = trainingSessionMapper;
    }

    @Cacheable(
            cacheNames = CacheNames.SESSION_SEARCH,
            key = "#request.cacheKey()",
            sync = true)
    @Transactional(readOnly = true)
    public PageResponse<TrainingSessionResponse> search(SessionSearchRequest request) {
        request.validateRanges();

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

    private String propertyFor(String requestedSort) {
        return switch (requestedSort.toLowerCase()) {
            case "price" -> "price";
            case "createdat" -> "createdAt";
            default -> "startDate";
        };
    }
}
