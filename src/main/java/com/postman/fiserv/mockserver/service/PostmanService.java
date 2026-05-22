package com.postman.fiserv.mockserver.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.postman.fiserv.mockserver.config.PostmanProperties;
import com.postman.fiserv.mockserver.model.SpecFileInfo;
import com.postman.fiserv.mockserver.model.persistence.PostmanResource;
import com.postman.fiserv.mockserver.model.persistence.PostmanTenant;
import com.postman.fiserv.mockserver.model.persistence.ResourceStatus;
import com.postman.fiserv.mockserver.model.postman.CollectionResponse;
import com.postman.fiserv.mockserver.model.postman.MockResponse;
import com.postman.fiserv.mockserver.model.postman.SpecResponse;
import com.postman.fiserv.mockserver.model.postman.SpecTaskResponse;
import com.postman.fiserv.mockserver.model.postman.WorkspaceResponse;
import com.postman.fiserv.mockserver.service.persistence.PostmanResourceService;

@Service
public class PostmanService {

    private static final Logger log = LoggerFactory.getLogger(PostmanService.class);

    private final PostmanApiClient apiClient;
    private final PostmanResourceService resourceService;
    private final PostmanProperties properties;

    public PostmanService(
            PostmanApiClient apiClient,
            PostmanResourceService resourceService,
            PostmanProperties properties) {
        this.apiClient = apiClient;
        this.resourceService = resourceService;
        this.properties = properties;
    }

    public PostmanResource handleAdded(String tenantName, String branchName, SpecFileInfo info) {
        // Step 0: pick up any existing in-progress doc (or start one).
        PostmanResource resource = resourceService.findResource(tenantName, branchName, info.filePath())
                .orElseGet(() -> resourceService.startProvisioning(tenantName, branchName, info));

        // Fully done from a previous run — idempotent no-op.
        if (resource.getStatus() == ResourceStatus.READY) {
            log.info("Resource for tenant={} branch={} path={} already READY — no-op",
                    tenantName, branchName, info.filePath());
            return resource;
        }

        PostmanTenant tenant = ensureTenant(tenantName);
        String workspaceId = tenant.getPostmanWorkspaceId();
        String resourceName = info.pathFromReference() + " - " + branchName;

        // Step 1: spec (skip if a prior run already created it).
        if (resource.getPostmanSpecId() == null) {
            SpecResponse spec = apiClient.createSpec(workspaceId, resourceName, info.openapiType(), info.rawYaml());
            resource.setPostmanSpecId(spec.id());
            resource = resourceService.save(resource);
        }

        // Step 2: collection (generate + extract both id + uid).
        if (resource.getPostmanCollectionUid() == null) {
            SpecTaskResponse genTask = apiClient.triggerCollectionGeneration(resource.getPostmanSpecId(), resourceName);
            SpecTaskResponse completed = apiClient.pollSpecTaskUntilComplete(resource.getPostmanSpecId(), genTask.taskId());
            assertTaskSucceeded(completed);
            String collectionUid = extractCollectionUid(completed);
            CollectionResponse collection = apiClient.getCollection(collectionUid);
            String collectionId = collection.collection().info().postmanId();
            String baseUrl = extractBaseUrl(collection);
            if (baseUrl == null) {
                log.warn("Collection {} has no 'baseUrl' variable", collectionUid);
            }
            resource.setPostmanCollectionUid(collectionUid);
            resource.setPostmanCollectionId(collectionId);
            resource.setPostmanCollectionBaseUrl(baseUrl);
            resource = resourceService.save(resource);
        }

        // Step 3: mock.
        if (resource.getPostmanMockServerId() == null) {
            MockResponse.Mock mock = apiClient.createMock(workspaceId, resource.getPostmanCollectionUid(), resourceName);
            resource.setPostmanMockServerId(mock.id());
            resource.setPostmanMockServerUrl(mock.mockUrl());
            resource = resourceService.save(resource);
        }

        return resourceService.markResourceStatus(resource, ResourceStatus.READY, null);
    }

    public PostmanResource handleModified(PostmanResource existing, SpecFileInfo info) {
        resourceService.markResourceStatus(existing, ResourceStatus.SYNCING, null);
        apiClient.patchSpecFile(existing.getPostmanSpecId(), info.rawYaml());

        SpecTaskResponse syncTask = apiClient.triggerCollectionSync(
                existing.getPostmanCollectionUid(), existing.getPostmanSpecId());
        SpecTaskResponse completed = apiClient.pollCollectionTaskUntilComplete(
                existing.getPostmanCollectionUid(), syncTask.taskId());
        assertTaskSucceeded(completed);

        // Refresh baseUrl from the post-sync collection (the spec's servers block may have changed).
        CollectionResponse collection = apiClient.getCollection(existing.getPostmanCollectionUid());
        String baseUrl = extractBaseUrl(collection);
        if (baseUrl == null) {
            log.warn("Collection {} has no 'baseUrl' variable after sync", existing.getPostmanCollectionUid());
        }
        existing.setPostmanCollectionBaseUrl(baseUrl);

        return resourceService.markResourceStatus(existing, ResourceStatus.READY, null);
    }

    public void handleRemoved(PostmanResource existing) {
        apiClient.deleteMock(existing.getPostmanMockServerId());
        apiClient.deleteCollection(existing.getPostmanCollectionId());
        apiClient.deleteSpec(existing.getPostmanSpecId());
        resourceService.deleteResource(existing);
    }

    public void cleanupTenantIfEmpty(String tenantName) {
        if (resourceService.tenantHasAnyResource(tenantName)) {
            return;
        }
        resourceService.findTenant(tenantName).ifPresent(tenant -> {
            log.info("Tenant '{}' has no remaining resources — deleting workspace {} and tenant doc",
                    tenantName, tenant.getPostmanWorkspaceId());
            apiClient.deleteWorkspace(tenant.getPostmanWorkspaceId());
            resourceService.deleteTenant(tenantName);
        });
    }

    private PostmanTenant ensureTenant(String tenantName) {
        return resourceService.findTenant(tenantName).orElseGet(() -> {
            String workspaceName = tenantName + " - " + properties.environmentName();
            WorkspaceResponse.Workspace ws = apiClient.createWorkspace(workspaceName);
            return resourceService.createTenant(tenantName, ws.id());
        });
    }

    private void assertTaskSucceeded(SpecTaskResponse task) {
        String status = task.status();
        if (status == null) {
            throw new IllegalStateException("Postman task has null status: " + task);
        }
        String normalized = status.toLowerCase();
        if (normalized.contains("fail") || normalized.contains("error")) {
            throw new IllegalStateException("Postman task ended in failure status '" + status + "': " + task);
        }
    }

    private String extractBaseUrl(CollectionResponse collection) {
        if (collection == null || collection.collection() == null) {
            return null;
        }
        List<CollectionResponse.Collection.Variable> variables = collection.collection().variable();
        if (variables == null) {
            return null;
        }
        for (CollectionResponse.Collection.Variable variable : variables) {
            if ("baseUrl".equals(variable.key())) {
                return variable.value();
            }
        }
        return null;
    }

    private String extractCollectionUid(SpecTaskResponse task) {
        // Postman returns { details: { resources: [{ url, id }] } } where id is the collection UID
        // (ownerId-collectionId format). The result field is unused for collection-generation tasks.
        Map<String, Object> details = task.details();
        if (details != null) {
            Object resources = details.get("resources");
            if (resources instanceof List<?> list && !list.isEmpty()
                    && list.get(0) instanceof Map<?, ?> resourceMap) {
                Object id = resourceMap.get("id");
                if (id != null) return id.toString();
            }
        }
        throw new IllegalStateException("Could not extract collection UID from task: " + task);
    }
}
