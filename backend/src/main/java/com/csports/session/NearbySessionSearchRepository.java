package com.csports.session;

import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.csports.session.dto.SessionSearchRequest;

@Repository
class NearbySessionSearchRepository {

    private static final String USER_POINT = """
            CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography)
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    NearbySessionSearchRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    NearbySessionPage findNearby(SessionSearchRequest request) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("latitude", request.latitude())
                .addValue("longitude", request.longitude())
                .addValue("radiusMeters", request.radiusMeters())
                .addValue("fromDate", request.effectiveFromDate())
                .addValue("limit", request.size())
                .addValue("offset", (long) request.page() * request.size());

        String whereClause = buildWhereClause(request, parameters);
        String direction = "desc".equalsIgnoreCase(request.direction()) ? "DESC" : "ASC";

        String pageSql = """
                SELECT ts.id,
                       ST_Distance(ts.location, %s) AS distance_meters
                FROM training_session ts
                %s
                ORDER BY distance_meters %s, ts.id ASC
                LIMIT :limit OFFSET :offset
                """.formatted(USER_POINT, whereClause, direction);

        List<NearbySessionMatch> matches = jdbcTemplate.query(
                pageSql,
                parameters,
                (resultSet, rowNumber) -> new NearbySessionMatch(
                        resultSet.getLong("id"),
                        resultSet.getDouble("distance_meters")));

        Long totalElements = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM training_session ts " + whereClause,
                parameters,
                Long.class);

        return new NearbySessionPage(
                List.copyOf(matches),
                totalElements == null ? 0 : totalElements);
    }

    private String buildWhereClause(
            SessionSearchRequest request,
            MapSqlParameterSource parameters) {
        StringBuilder where = new StringBuilder("""
                WHERE ts.status = 'SCHEDULED'
                  AND ts.end_date >= :fromDate
                  AND ST_DWithin(ts.location, %s, :radiusMeters)
                """.formatted(USER_POINT));

        if (request.toDate() != null) {
            where.append(" AND ts.start_date <= :toDate");
            parameters.addValue("toDate", request.toDate());
        }
        if (request.sportId() != null) {
            where.append(" AND ts.sport_id = :sportId");
            parameters.addValue("sportId", request.sportId());
        }
        if (request.trainerId() != null) {
            where.append(" AND ts.trainer_id = :trainerId");
            parameters.addValue("trainerId", request.trainerId());
        }
        if (request.minPrice() != null) {
            where.append(" AND ts.price >= :minPrice");
            parameters.addValue("minPrice", request.minPrice());
        }
        if (request.maxPrice() != null) {
            where.append(" AND ts.price <= :maxPrice");
            parameters.addValue("maxPrice", request.maxPrice());
        }
        if (request.availableOnly()) {
            where.append(" AND ts.current_participants < ts.max_participants");
        }
        if (request.q() != null) {
            where.append("""
                     AND (
                         POSITION(:query IN LOWER(ts.title)) > 0
                         OR POSITION(:query IN LOWER(ts.description)) > 0
                         OR POSITION(:query IN LOWER(ts.location_name)) > 0
                     )
                    """);
            parameters.addValue("query", request.q());
        }

        return where.toString();
    }
}
