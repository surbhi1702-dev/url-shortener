package com.surbhi.urlshortener.service;

import com.surbhi.urlshortener.exception.InvalidUrlException;
import com.surbhi.urlshortener.exception.UrlExpiredException;
import com.surbhi.urlshortener.exception.UrlNotFoundException;
import com.surbhi.urlshortener.model.Url;
import com.surbhi.urlshortener.repository.UrlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class UrlService {

    private static final int MAX_ATTEMPTS = 5;

    private final UrlRepository urlRepository;
    private final ShortCodeGenerator codeGenerator;
    private final Clock clock;

    public UrlService(UrlRepository urlRepository, ShortCodeGenerator codeGenerator, Clock clock) {
        this.urlRepository = urlRepository;
        this.codeGenerator = codeGenerator;
        this.clock = clock;
    }

    @Transactional
    public Url shorten(String originalUrl, Integer expiresInDays) {
        String url = validate(originalUrl.trim());
        Instant expiresAt = expiresInDays == null ? null : clock.instant().plus(Duration.ofDays(expiresInDays));
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = codeGenerator.generate();
            if (!urlRepository.existsByShortCode(code)) {
                return urlRepository.save(new Url(code, url, expiresAt));
            }
        }
        throw new IllegalStateException("Could not generate a unique short code");
    }

    /** Looks up a link for redirecting; throws if it is unknown or expired. */
    @Transactional(readOnly = true)
    public Url resolve(String shortCode) {
        Url url = find(shortCode);
        if (url.isExpired(clock.instant())) {
            throw new UrlExpiredException(shortCode);
        }
        return url;
    }

    /** Looks up a link regardless of expiry, for stats. */
    @Transactional(readOnly = true)
    public Url find(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
    }

    private String validate(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null) {
                throw new InvalidUrlException("url must be an absolute http or https URL");
            }
            return url;
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("url is not valid: " + e.getMessage());
        }
    }
}
