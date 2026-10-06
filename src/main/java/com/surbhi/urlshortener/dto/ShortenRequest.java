package com.surbhi.urlshortener.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param expiresInDays optional; how many days the link stays valid. Omit for a link that never expires.
 */
public record ShortenRequest(
        @NotBlank(message = "url is required")
        @Size(max = 2048, message = "url must be at most 2048 characters")
        String url,

        @Min(value = 1, message = "expiresInDays must be at least 1")
        @Max(value = 3650, message = "expiresInDays must be at most 3650")
        Integer expiresInDays) {
}
