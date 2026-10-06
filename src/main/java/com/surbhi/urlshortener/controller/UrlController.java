package com.surbhi.urlshortener.controller;

import com.surbhi.urlshortener.dto.ShortenRequest;
import com.surbhi.urlshortener.dto.ShortenResponse;
import com.surbhi.urlshortener.dto.UrlStatsResponse;
import com.surbhi.urlshortener.model.Url;
import com.surbhi.urlshortener.service.ClickService;
import com.surbhi.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
public class UrlController {

    private final UrlService urlService;
    private final ClickService clickService;

    public UrlController(UrlService urlService, ClickService clickService) {
        this.urlService = urlService;
        this.clickService = clickService;
    }

    @PostMapping("/api/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        Url url = urlService.shorten(request.url(), request.expiresInDays());
        URI shortUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/{code}")
                .buildAndExpand(url.getShortCode())
                .toUri();
        ShortenResponse body = new ShortenResponse(url.getShortCode(), shortUri.toString(), url.getOriginalUrl(),
                url.getCreatedAt(), url.getExpiresAt());
        return ResponseEntity.created(shortUri).body(body);
    }

    @GetMapping("/api/urls/{code}/stats")
    public UrlStatsResponse stats(@PathVariable String code) {
        return clickService.stats(urlService.find(code));
    }

    @GetMapping("/{code:[a-zA-Z0-9]+}")
    public ResponseEntity<Void> redirect(@PathVariable String code,
                                         @RequestHeader(value = HttpHeaders.REFERER, required = false) String referrer,
                                         @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        Url url = urlService.resolve(code);
        clickService.recordClick(url, referrer, userAgent);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url.getOriginalUrl()))
                .build();
    }
}
