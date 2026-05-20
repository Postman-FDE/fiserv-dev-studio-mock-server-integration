package com.postman.fiserv.mockserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "postman")
public record PostmanProperties(
        String apiKey,
        String baseUrl,
        String environmentName,
        SpecTask specTask,
        RateLimit rateLimit
) {
    public record SpecTask(long pollIntervalMs, long timeoutMs) {}

    public record RateLimit(
            int maxAttempts,
            long jitterMs,
            long initialBackoffMs,
            long maxBackoffMs,
            double multiplier
    ) {}
}
