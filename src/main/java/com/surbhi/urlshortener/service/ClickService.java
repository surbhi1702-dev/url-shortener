package com.surbhi.urlshortener.service;

import com.surbhi.urlshortener.dto.UrlStatsResponse;
import com.surbhi.urlshortener.dto.UrlStatsResponse.ReferrerCount;
import com.surbhi.urlshortener.model.Click;
import com.surbhi.urlshortener.model.Url;
import com.surbhi.urlshortener.repository.ClickRepository;
import com.surbhi.urlshortener.repository.UrlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ClickService {

    static final int STATS_DAYS = 7;
    private static final int TOP_REFERRERS = 5;

    private final ClickRepository clickRepository;
    private final UrlRepository urlRepository;
    private final Clock clock;

    public ClickService(ClickRepository clickRepository, UrlRepository urlRepository, Clock clock) {
        this.clickRepository = clickRepository;
        this.urlRepository = urlRepository;
        this.clock = clock;
    }

    @Transactional
    public void recordClick(Url url, String referrer, String userAgent) {
        urlRepository.incrementClickCount(url.getId());
        clickRepository.save(new Click(url, clock.instant(), referrer, userAgent));
    }

    /** Totals, last click, clicks per day (UTC) for the last 7 days, and top referrers. */
    @Transactional(readOnly = true)
    public UrlStatsResponse stats(Url url) {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        LocalDate firstDay = today.minusDays(STATS_DAYS - 1);

        Map<LocalDate, Long> perDay = new TreeMap<>();
        for (LocalDate day = firstDay; !day.isAfter(today); day = day.plusDays(1)) {
            perDay.put(day, 0L);
        }
        Instant since = firstDay.atStartOfDay(ZoneOffset.UTC).toInstant();
        for (Click click : clickRepository.findByUrlAndClickedAtGreaterThanEqual(url, since)) {
            perDay.merge(LocalDate.ofInstant(click.getClickedAt(), ZoneOffset.UTC), 1L, Long::sum);
        }

        List<ReferrerCount> referrers = clickRepository.countByReferrer(url).stream()
                .limit(TOP_REFERRERS)
                .map(row -> new ReferrerCount((String) row[0], (Long) row[1]))
                .toList();

        Instant lastClickedAt = clickRepository.findFirstByUrlOrderByClickedAtDesc(url)
                .map(Click::getClickedAt)
                .orElse(null);

        return new UrlStatsResponse(url.getShortCode(), url.getOriginalUrl(), url.getCreatedAt(),
                url.getExpiresAt(), url.isExpired(now), url.getClickCount(), lastClickedAt, perDay, referrers);
    }
}
