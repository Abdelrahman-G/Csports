package com.csports.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    /**
     * Atomically consumes a refresh token.
     *
     * <p>The revoked predicate is evaluated by the database while it holds the
     * row lock. Consequently, concurrent refresh requests cannot both consume
     * the same token successfully.</p>
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update RefreshToken refreshToken
               set refreshToken.revoked = true
             where refreshToken.id = :id
               and refreshToken.revoked = false
            """)
    int revokeIfActive(@Param("id") Long id);
}
