package com.postman.fiserv.mockserver.model.postman;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Unified shape covering both the trigger-and-poll lifecycle:
 *   - Trigger responses (POST /specs/:id/generations/:type, PUT /collections/:uid/synchronizations)
 *     return { taskId, url }.
 *   - Poll responses (GET /specs/:specId/tasks/:taskId) return the task state including status + result.
 * Unused fields are null for whichever side of the lifecycle you're on.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SpecTaskResponse(
        String taskId,
        String url,
        String id,
        String status,
        Map<String, Object> result,
        Map<String, Object> details
) {}
