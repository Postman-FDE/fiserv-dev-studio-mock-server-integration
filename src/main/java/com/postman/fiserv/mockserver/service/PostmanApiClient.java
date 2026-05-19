package com.postman.fiserv.mockserver.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.postman.fiserv.mockserver.config.PostmanProperties;
import com.postman.fiserv.mockserver.model.postman.CollectionResponse;
import com.postman.fiserv.mockserver.model.postman.CreateMockRequest;
import com.postman.fiserv.mockserver.model.postman.CreateSpecRequest;
import com.postman.fiserv.mockserver.model.postman.CreateWorkspaceRequest;
import com.postman.fiserv.mockserver.model.postman.GenerateCollectionRequest;
import com.postman.fiserv.mockserver.model.postman.MockResponse;
import com.postman.fiserv.mockserver.model.postman.SpecResponse;
import com.postman.fiserv.mockserver.model.postman.SpecTaskResponse;
import com.postman.fiserv.mockserver.model.postman.UpdateSpecFileRequest;
import com.postman.fiserv.mockserver.model.postman.WorkspaceResponse;

@Component
public class PostmanApiClient {

    public static final String SPEC_FILE_PATH = "openapi.yaml";
    private static final String WORKSPACE_TYPE_PRIVATE = "private";
    private static final String TASK_STATUS_PENDING = "pending";

    private static final Logger log = LoggerFactory.getLogger(PostmanApiClient.class);

    private final RestClient client;
    private final PostmanProperties properties;

    public PostmanApiClient(RestClient postmanRestClient, PostmanProperties properties) {
        this.client = postmanRestClient;
        this.properties = properties;
    }

    public WorkspaceResponse.Workspace createWorkspace(String name) {
        log.info("Postman: creating workspace '{}'", name);
        CreateWorkspaceRequest body = new CreateWorkspaceRequest(
                new CreateWorkspaceRequest.Workspace(name, WORKSPACE_TYPE_PRIVATE, null));
        WorkspaceResponse response = client.post()
                .uri("/workspaces")
                .body(body)
                .retrieve()
                .body(WorkspaceResponse.class);
        return response.workspace();
    }

    public SpecResponse createSpec(String workspaceId, String name, String openapiType, String yamlContent) {
        log.info("Postman: creating spec '{}' (type={}) in workspace {}", name, openapiType, workspaceId);
        CreateSpecRequest body = new CreateSpecRequest(
                name,
                openapiType,
                List.of(new CreateSpecRequest.SpecFile(SPEC_FILE_PATH, yamlContent)));
        return client.post()
                .uri(uriBuilder -> uriBuilder.path("/specs").queryParam("workspaceId", workspaceId).build())
                .body(body)
                .retrieve()
                .body(SpecResponse.class);
    }

    public SpecTaskResponse triggerCollectionGeneration(String specId, String collectionName) {
        log.info("Postman: triggering collection generation for spec {} (collection name '{}')", specId, collectionName);
        Map<String, Object> options = Map.of(
                "requestNameSource", "Fallback",
                "indentCharacter", "Space",
                "folderStrategy", "Paths",
                "includeAuthInfoInExample", true,
                "enableOptionalParameters", true,
                "keepImplicitHeaders", false,
                "includeDeprecated", true,
                "alwaysInheritAuthentication", false,
                "nestedFolderHierarchy", false
        );
        GenerateCollectionRequest body = new GenerateCollectionRequest(collectionName, options);
        SpecTaskResponse response = client.post()
                .uri("/specs/{specId}/generations/collection", specId)
                .body(body)
                .retrieve()
                .body(SpecTaskResponse.class);
        log.info("Postman: generation triggered, taskId={}, url={}", response.taskId(), response.url());
        return response;
    }

    public CollectionResponse getCollection(String collectionId) {
        return client.get()
                .uri("/collections/{collectionId}", collectionId)
                .retrieve()
                .body(CollectionResponse.class);
    }

    public MockResponse.Mock createMock(String workspaceId, String collectionUid, String name) {
        log.info("Postman: creating mock '{}' for collection {}", name, collectionUid);
        CreateMockRequest body = new CreateMockRequest(new CreateMockRequest.Mock(name, collectionUid, false));
        MockResponse response = client.post()
                .uri(uriBuilder -> uriBuilder.path("/mocks").queryParam("workspace", workspaceId).build())
                .body(body)
                .retrieve()
                .body(MockResponse.class);
        return response.mock();
    }

    public SpecResponse patchSpecFile(String specId, String yamlContent) {
        log.info("Postman: PATCH spec {} file {}", specId, SPEC_FILE_PATH);
        UpdateSpecFileRequest body = new UpdateSpecFileRequest(yamlContent);
        return client.patch()
                .uri("/specs/{specId}/files/{path}", specId, SPEC_FILE_PATH)
                .body(body)
                .retrieve()
                .body(SpecResponse.class);
    }

    public SpecTaskResponse triggerCollectionSync(String collectionUid, String specId) {
        log.info("Postman: triggering collection sync for collection {} from spec {}", collectionUid, specId);
        // PUT /collections/:uid/synchronizations takes no body and returns { taskId, url }.
        SpecTaskResponse response = client.put()
                .uri(uriBuilder -> uriBuilder
                        .path("/collections/{collectionUid}/synchronizations")
                        .queryParam("specId", specId)
                        .build(collectionUid))
                .retrieve()
                .body(SpecTaskResponse.class);
        log.info("Postman: sync triggered, taskId={}, url={}", response.taskId(), response.url());
        return response;
    }

    public void deleteMock(String mockId) {
        log.info("Postman: deleting mock {}", mockId);
        client.delete().uri("/mocks/{id}", mockId).retrieve().toBodilessEntity();
    }

    public void deleteCollection(String collectionId) {
        log.info("Postman: deleting collection {}", collectionId);
        client.delete().uri("/collections/{id}", collectionId).retrieve().toBodilessEntity();
    }

    public void deleteSpec(String specId) {
        log.info("Postman: deleting spec {}", specId);
        client.delete().uri("/specs/{id}", specId).retrieve().toBodilessEntity();
    }

    public void deleteWorkspace(String workspaceId) {
        log.info("Postman: deleting workspace {}", workspaceId);
        client.delete().uri("/workspaces/{id}", workspaceId).retrieve().toBodilessEntity();
    }

    public SpecTaskResponse getSpecTask(String specId, String taskId) {
        return client.get()
                .uri("/specs/{specId}/tasks/{taskId}", specId, taskId)
                .retrieve()
                .body(SpecTaskResponse.class);
    }

    /** Used by the added flow (collection generation). Polls under /specs/{specId}/tasks/{taskId}. */
    public SpecTaskResponse pollSpecTaskUntilComplete(String specId, String taskId) {
        long pollIntervalMs = properties.specTask().pollIntervalMs();
        long timeoutMs = properties.specTask().timeoutMs();
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (true) {
            SpecTaskResponse task = getSpecTask(specId, taskId);
            String status = task.status();
            if (status != null && !TASK_STATUS_PENDING.equalsIgnoreCase(status)) {
                log.info("Postman: spec task {} reached terminal status '{}'", taskId, status);
                return task;
            }
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException(
                        "Spec task " + taskId + " did not complete within " + timeoutMs + "ms (last status=" + status + ")");
            }
            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while polling spec task " + taskId, e);
            }
        }
    }

    /**
     * Used by the modified flow (collection sync). Sync tasks are queryable under
     * /collections/{collectionUid}/tasks/{taskId} per the Postman task-status endpoint
     * (which accepts elementType=specs|collections). Polling under /specs/... 404s for sync.
     */
    public SpecTaskResponse pollCollectionTaskUntilComplete(String collectionUid, String taskId) {
        long pollIntervalMs = properties.specTask().pollIntervalMs();
        long timeoutMs = properties.specTask().timeoutMs();
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (true) {
            SpecTaskResponse task = client.get()
                    .uri("/collections/{collectionUid}/tasks/{taskId}", collectionUid, taskId)
                    .retrieve()
                    .body(SpecTaskResponse.class);
            String status = task.status();
            if (status != null && !TASK_STATUS_PENDING.equalsIgnoreCase(status)) {
                log.info("Postman: collection task {} reached terminal status '{}'", taskId, status);
                return task;
            }
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException(
                        "Collection task " + taskId + " did not complete within " + timeoutMs + "ms (last status=" + status + ")");
            }
            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while polling collection task " + taskId, e);
            }
        }
    }
}
