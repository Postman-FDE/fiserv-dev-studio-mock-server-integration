package com.postman.fiserv.mockserver.config;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

/**
 * Transparent 429 retry for Postman API calls. Registered on the postmanRestClient bean so
 * every outgoing Postman request runs through it. On 429:
 *  - Read X-RateLimit-RetryAfter (Postman's non-standard header, in seconds). Falls back to
 *    the RetryAfter alias, then to a computed exponential backoff if headers are absent.
 *  - Add small random jitter to avoid thundering-herd when parallel events trigger retries.
 *  - Apply exponential growth across retry attempts on the same call (sliding window can re-bite).
 *  - Cap each individual sleep and total attempts so a misbehaving rate limiter can't hang the
 *    request thread indefinitely.
 * On exhaustion, returns the final 429 response unchanged — RestClient.retrieve().body() will
 * then throw HttpClientErrorException.TooManyRequests, which EventService records as FAILED
 * (same error path as before this interceptor existed).
 */
@Component
public class PostmanRateLimitInterceptor implements ClientHttpRequestInterceptor {

    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final String HDR_RETRY_AFTER = "X-RateLimit-RetryAfter";
    private static final String HDR_RETRY_AFTER_ALIAS = "RetryAfter";
    private static final String HDR_LIMIT = "X-RateLimit-Limit";
    private static final String HDR_REMAINING = "X-RateLimit-Remaining";
    private static final String HDR_RESET = "X-RateLimit-Reset";

    private static final Logger log = LoggerFactory.getLogger(PostmanRateLimitInterceptor.class);

    private final PostmanProperties.RateLimit config;

    public PostmanRateLimitInterceptor(PostmanProperties properties) {
        this.config = properties.rateLimit();
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        ClientHttpResponse response = null;
        for (int attempt = 0; attempt < config.maxAttempts(); attempt++) {
            response = execution.execute(request, body);
            if (response.getStatusCode().value() != HTTP_TOO_MANY_REQUESTS) {
                return response;
            }

            HttpHeaders headers = response.getHeaders();
            long sleepMs = computeSleepMs(attempt, headers);
            log.warn("Postman 429 on {} {} (attempt {}/{}). limit={}, remaining={}, reset={}, retryAfter={}. Sleeping {}ms.",
                    request.getMethod(), request.getURI().getPath(),
                    attempt + 1, config.maxAttempts(),
                    headers.getFirst(HDR_LIMIT),
                    headers.getFirst(HDR_REMAINING),
                    headers.getFirst(HDR_RESET),
                    headers.getFirst(HDR_RETRY_AFTER),
                    sleepMs);

            response.close();

            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for Postman rate-limit retry", e);
            }
        }

        log.error("Postman 429 not cleared after {} attempts on {} {} — propagating to caller",
                config.maxAttempts(), request.getMethod(), request.getURI().getPath());
        return response;
    }

    private long computeSleepMs(int attempt, HttpHeaders headers) {
        Long serverWaitSec = parseLong(headers.getFirst(HDR_RETRY_AFTER));
        if (serverWaitSec == null) {
            serverWaitSec = parseLong(headers.getFirst(HDR_RETRY_AFTER_ALIAS));
        }

        long jitter = ThreadLocalRandom.current().nextLong(0, config.jitterMs() + 1);

        if (serverWaitSec != null && serverWaitSec > 0) {
            double factor = Math.pow(config.multiplier(), attempt);
            long baseMs = (long) (serverWaitSec * 1000L * factor);
            return Math.min(baseMs + jitter, config.maxBackoffMs());
        }

        long computed = (long) (config.initialBackoffMs() * Math.pow(config.multiplier(), attempt));
        long capped = Math.min(computed, config.maxBackoffMs());
        return ThreadLocalRandom.current().nextLong(0, capped + 1);
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
