package com.surbhi.urlshortener.service;

import com.surbhi.urlshortener.repository.ClickRepository;
import com.surbhi.urlshortener.repository.UrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Deletes links (and their clicks) once they have been expired for longer than the retention period.
 * Expired links stop redirecting immediately; keeping them for a while lets stats stay readable.
 */
@Component
public class ExpiredUrlCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(ExpiredUrlCleanupJob.class);

    private final UrlRepository urlRepository;
    private final ClickRepository clickRepository;
    private final Clock clock;
    private final Duration retention;

    public ExpiredUrlCleanupJob(UrlRepository urlRepository, ClickRepository clickRepository, Clock clock,
                                @Value("${app.expired-retention-days:30}") long retentionDays) {
        this.urlRepository = urlRepository;
        this.clickRepository = clickRepository;
        this.clock = clock;
        this.retention = Duration.ofDays(retentionDays);
    }

    @Scheduled(cron = "${app.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public int purgeExpired() {
        List<Long> ids = urlRepository.findIdsExpiredBefore(clock.instant().minus(retention));
        if (ids.isEmpty()) {
            return 0;
        }
        clickRepository.deleteByUrlIds(ids);
        urlRepository.deleteAllByIdInBatch(ids);
        log.info("Deleted {} expired links", ids.size());
        return ids.size();
    }
}
