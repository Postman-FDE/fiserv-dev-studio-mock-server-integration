package com.postman.fiserv.mockserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "postman")
public record PostmanProperties(
        String apiKey,
        String baseUrl,
        String environmentName,
        SpecTask specTask
) {
    public record SpecTask(long pollIntervalMs, long timeoutMs) {}
}
