package com.postman.fiserv.mockserver.service;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postman.fiserv.mockserver.model.RunApiRequest;
import com.postman.fiserv.mockserver.model.persistence.PostmanResource;
import com.postman.fiserv.mockserver.service.persistence.PostmanResourceService;

@Service
public class RunApiRequestService {

    private static final Logger log = LoggerFactory.getLogger(RunApiRequestService.class);

    private final PostmanResourceService resourceService;
    private final RestClient mockClient;
    private final ObjectMapper objectMapper;

    public RunApiRequestService(
            PostmanResourceService resourceService,
            RestClient mockServerRestClient,
            ObjectMapper objectMapper) {
        this.resourceService = resourceService;
        this.mockClient = mockServerRestClient;
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<byte[]> run(RunApiRequest request) {
        Optional<PostmanResource> maybe = resourceService.findResource(
                request.productName(), request.branchName(), request.filePath());
        if (maybe.isEmpty()) {
            log.warn("RunApi: no resource for product={} branch={} path={}",
                    request.productName(), request.branchName(), request.filePath());
            return jsonErrorResponse(HttpStatus.NOT_FOUND, "resource not found", request);
        }

        PostmanResource resource = maybe.get();
        String mockUrl = resource.getPostmanMockServerUrl();
        if (mockUrl == null || mockUrl.isBlank()) {
            log.error("RunApi: resource for product={} branch={} path={} has no mockServerUrl",
                    request.productName(), request.branchName(), request.filePath());
            return jsonErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "resource has no mock server URL", request);
        }

        String fullUrl = composeUrl(mockUrl, resource.getPostmanCollectionBaseUrl(), request.requestPath());
        HttpMethod method = HttpMethod.valueOf(request.requestType().toUpperCase(Locale.ROOT));
        byte[] body = serializeBody(request.requestBody());

        log.info("RunApi: {} {} (resource={})", method, fullUrl, resource.getFilePath());

        RestClient.RequestBodySpec spec = mockClient.method(method)
                .uri(URI.create(fullUrl))
                .headers(headers -> copyHeaders(request.requestHeaders(), headers));

        RestClient.RequestHeadersSpec<?> bodied = body.length > 0 ? spec.body(body) : spec;

        return bodied.exchange((req, resp) -> {
            byte[] respBody = resp.getBody().readAllBytes();
            ResponseEntity.BodyBuilder builder = ResponseEntity.status(resp.getStatusCode());
            MediaType contentType = resp.getHeaders().getContentType();
            if (contentType != null) {
                builder.contentType(contentType);
            }
            return builder.body(respBody);
        });
    }

    private String composeUrl(String mockUrl, String baseUrl, String requestPath) {
        String basePath = "";
        if (baseUrl != null && !baseUrl.isBlank()) {
            try {
                String p = URI.create(baseUrl).getPath();
                if (p != null && !p.isEmpty() && !p.equals("/")) {
                    basePath = p.endsWith("/") ? p.substring(0, p.length() - 1) : p;
                }
            } catch (IllegalArgumentException e) {
                log.warn("Could not parse postmanCollectionBaseUrl '{}'; composing without base path", baseUrl);
            }
        } else {
            log.warn("postmanCollectionBaseUrl is null/blank for this resource; composing without base path");
        }
        String trimmedMock = mockUrl.endsWith("/") ? mockUrl.substring(0, mockUrl.length() - 1) : mockUrl;
        String reqPath = requestPath == null || requestPath.isBlank()
                ? "/"
                : (requestPath.startsWith("/") ? requestPath : "/" + requestPath);
        return trimmedMock + basePath + reqPath;
    }

    private byte[] serializeBody(JsonNode body) {
        if (body == null || body.isNull()) {
            return new byte[0];
        }
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize request body", e);
        }
    }

    private void copyHeaders(Map<String, String> source, HttpHeaders target) {
        if (source == null) {
            return;
        }
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                continue;
            }
            String lower = key.toLowerCase(Locale.ROOT);
            if (lower.equals("host") || lower.equals("content-length")) {
                continue;
            }
            target.add(key, entry.getValue());
        }
    }

    private ResponseEntity<byte[]> jsonErrorResponse(HttpStatus status, String message, RunApiRequest request) {
        Map<String, String> errorBody = Map.of(
                "error", message,
                "productName", nullToEmpty(request.productName()),
                "branchName", nullToEmpty(request.branchName()),
                "filePath", nullToEmpty(request.filePath())
        );
        try {
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsBytes(errorBody));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize error body", e);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
