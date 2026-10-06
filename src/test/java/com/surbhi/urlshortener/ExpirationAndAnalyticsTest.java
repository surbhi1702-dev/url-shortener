package com.surbhi.urlshortener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.surbhi.urlshortener.model.Url;
import com.surbhi.urlshortener.repository.UrlRepository;
import com.surbhi.urlshortener.service.ExpiredUrlCleanupJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExpirationAndAnalyticsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private ExpiredUrlCleanupJob cleanupJob;

    @Test
    void shortenWithExpirySetsExpiresAt() throws Exception {
        String response = mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\",\"expiresInDays\":7}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Instant expiresAt = Instant.parse(objectMapper.readTree(response).get("expiresAt").asText());
        assertThat(Duration.between(Instant.now(), expiresAt)).isBetween(Duration.ofDays(7).minusMinutes(1),
                Duration.ofDays(7));
    }

    @Test
    void rejectsInvalidExpiry() throws Exception {
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\",\"expiresInDays\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void expiredLinkReturns410AndIsNotCounted() throws Exception {
        urlRepository.save(new Url("expired1", "https://example.com", Instant.now().minusSeconds(60)));

        mockMvc.perform(get("/expired1")).andExpect(status().isGone());

        mockMvc.perform(get("/api/urls/expired1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expired").value(true))
                .andExpect(jsonPath("$.totalClicks").value(0));
    }

    @Test
    void redirectsAreCountedInStats() throws Exception {
        String response = mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/stats\"}"))
                .andReturn().getResponse().getContentAsString();
        String code = objectMapper.readTree(response).get("shortCode").asText();

        mockMvc.perform(get("/" + code).header("Referer", "https://twitter.com")).andExpect(status().isFound());
        mockMvc.perform(get("/" + code).header("Referer", "https://twitter.com")).andExpect(status().isFound());
        mockMvc.perform(get("/" + code)).andExpect(status().isFound());

        String today = LocalDate.now(ZoneOffset.UTC).toString();
        String stats = mockMvc.perform(get("/api/urls/" + code + "/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(3))
                .andExpect(jsonPath("$.expired").value(false))
                .andExpect(jsonPath("$.lastClickedAt").isNotEmpty())
                .andExpect(jsonPath("$.topReferrers[0].referrer").value("https://twitter.com"))
                .andExpect(jsonPath("$.topReferrers[0].clicks").value(2))
                .andReturn().getResponse().getContentAsString();

        JsonNode perDay = objectMapper.readTree(stats).get("clicksPerDay");
        assertThat(perDay.size()).isEqualTo(7);
        assertThat(perDay.get(today).asLong()).isEqualTo(3);
    }

    @Test
    void statsForUnknownCodeReturns404() throws Exception {
        mockMvc.perform(get("/api/urls/nope404/stats")).andExpect(status().isNotFound());
    }

    @Test
    void cleanupDeletesLinksPastRetentionOnly() throws Exception {
        Url old = urlRepository.save(new Url("oldlink1", "https://example.com",
                Instant.now().minus(Duration.ofDays(40))));
        Url recent = urlRepository.save(new Url("newlink1", "https://example.com",
                Instant.now().minus(Duration.ofDays(1))));

        cleanupJob.purgeExpired();

        assertThat(urlRepository.findById(old.getId())).isEmpty();
        assertThat(urlRepository.findById(recent.getId())).isPresent();
    }
}
