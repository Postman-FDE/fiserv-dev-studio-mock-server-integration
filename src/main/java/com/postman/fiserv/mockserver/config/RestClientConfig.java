package com.postman.fiserv.mockserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient postmanRestClient(PostmanProperties properties, PostmanRateLimitInterceptor rateLimitInterceptor) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("X-API-Key", properties.apiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor(rateLimitInterceptor)
                .build();
    }

    /**
     * Plain RestClient used to forward UI sandbox requests to provisioned Postman mock servers.
     * No base URL (each call provides absolute), no Postman API key (mock.pstmn.io doesn't need it),
     * and no rate-limit interceptor (the Postman per-key budget covers api.getpostman.com, not the mocks).
     */
    @Bean
    public RestClient mockServerRestClient() {
        return RestClient.builder().build();
    }
}
