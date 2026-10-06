package com.surbhi.urlshortener.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Table(name = "urls", indexes = @Index(name = "idx_urls_expires_at", columnList = "expires_at"))
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, unique = true, length = 16)
    private String shortCode;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** When the link stops redirecting. Null means it never expires. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    // Default lets existing rows upgrade cleanly when the column is added.
    @ColumnDefault("0")
    @Column(name = "click_count", nullable = false)
    private long clickCount;

    protected Url() {
    }

    public Url(String shortCode, String originalUrl, Instant expiresAt) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }

    public Long getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public long getClickCount() {
        return clickCount;
    }
}
