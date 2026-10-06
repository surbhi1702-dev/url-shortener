package com.surbhi.urlshortener.repository;

import com.surbhi.urlshortener.model.Click;
import com.surbhi.urlshortener.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClickRepository extends JpaRepository<Click, Long> {

    List<Click> findByUrlAndClickedAtGreaterThanEqual(Url url, Instant since);

    Optional<Click> findFirstByUrlOrderByClickedAtDesc(Url url);

    @Query("select c.referrer, count(c) from Click c where c.url = :url and c.referrer is not null "
            + "group by c.referrer order by count(c) desc")
    List<Object[]> countByReferrer(@Param("url") Url url);

    @Modifying
    @Query("delete from Click c where c.url.id in :urlIds")
    void deleteByUrlIds(@Param("urlIds") Collection<Long> urlIds);
}
