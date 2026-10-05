package com.surbhi.urlshortener.controller;

import com.surbhi.urlshortener.dto.ShortenRequest;
import com.surbhi.urlshortener.dto.ShortenResponse;
import com.surbhi.urlshortener.model.Url;
import com.surbhi.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping("/api/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        Url url = urlService.shorten(request.url());
        URI shortUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/{code}")
                .buildAndExpand(url.getShortCode())
                .toUri();
        ShortenResponse body = new ShortenResponse(
                url.getShortCode(), shortUri.toString(), url.getOriginalUrl(), url.getCreatedAt());
        return ResponseEntity.created(shortUri).body(body);
    }

    @GetMapping("/{code:[a-zA-Z0-9]+}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        Url url = urlService.resolve(code);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url.getOriginalUrl()))
                .build();
    }
}
