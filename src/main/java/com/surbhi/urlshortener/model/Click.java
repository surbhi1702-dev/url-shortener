package com.surbhi.urlshortener.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** One redirect of a short link, used for click analytics. */
@Entity
@Table(name = "clicks", indexes = @Index(name = "idx_clicks_url_clicked_at", columnList = "url_id, clicked_at"))
public class Click {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "url_id", nullable = false)
    private Url url;

    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt;

    @Column(name = "referrer", length = 512)
    private String referrer;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    protected Click() {
    }

    public Click(Url url, Instant clickedAt, String referrer, String userAgent) {
        this.url = url;
        this.clickedAt = clickedAt;
        this.referrer = truncate(referrer);
        this.userAgent = truncate(userAgent);
    }

    private static String truncate(String value) {
        return value == null || value.length() <= 512 ? value : value.substring(0, 512);
    }

    public Instant getClickedAt() {
        return clickedAt;
    }

    public String getReferrer() {
        return referrer;
    }
}
