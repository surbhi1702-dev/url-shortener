package com.surbhi.urlshortener.repository;

import com.surbhi.urlshortener.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    /** Atomic increment, so concurrent redirects never lose a count. */
    @Modifying
    @Query("update Url u set u.clickCount = u.clickCount + 1 where u.id = :id")
    void incrementClickCount(@Param("id") Long id);

    @Query("select u.id from Url u where u.expiresAt is not null and u.expiresAt < :cutoff")
    List<Long> findIdsExpiredBefore(@Param("cutoff") Instant cutoff);
}
