package com.surbhi.urlshortener.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record UrlStatsResponse(
        String shortCode,
        String originalUrl,
        Instant createdAt,
        Instant expiresAt,
        boolean expired,
        long totalClicks,
        Instant lastClickedAt,
        Map<LocalDate, Long> clicksPerDay,
        List<ReferrerCount> topReferrers) {

    public record ReferrerCount(String referrer, long clicks) {
    }
}
