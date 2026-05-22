package com.postman.fiserv.mockserver.model;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public record RunApiRequest(
        String filePath,    // full webhook path (includes version)
        String branchName,
        String productName, // tenant name
        String requestType, // HTTP method
        String requestPath,
        JsonNode requestBody,
        Map<String, String> requestHeaders
) {}
