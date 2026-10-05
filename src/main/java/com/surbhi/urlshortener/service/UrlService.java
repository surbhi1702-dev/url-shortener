package com.surbhi.urlshortener.service;

import com.surbhi.urlshortener.exception.InvalidUrlException;
import com.surbhi.urlshortener.exception.UrlNotFoundException;
import com.surbhi.urlshortener.model.Url;
import com.surbhi.urlshortener.repository.UrlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class UrlService {

    private static final int MAX_ATTEMPTS = 5;

    private final UrlRepository urlRepository;
    private final ShortCodeGenerator codeGenerator;

    public UrlService(UrlRepository urlRepository, ShortCodeGenerator codeGenerator) {
        this.urlRepository = urlRepository;
        this.codeGenerator = codeGenerator;
    }

    @Transactional
    public Url shorten(String originalUrl) {
        String url = validate(originalUrl.trim());
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = codeGenerator.generate();
            if (!urlRepository.existsByShortCode(code)) {
                return urlRepository.save(new Url(code, url));
            }
        }
        throw new IllegalStateException("Could not generate a unique short code");
    }

    @Transactional(readOnly = true)
    public Url resolve(String shortCode) {
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
