package com.postman.fiserv.mockserver.service.persistence;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.postman.fiserv.mockserver.model.SpecFileInfo;
import com.postman.fiserv.mockserver.model.persistence.PostmanResource;
import com.postman.fiserv.mockserver.model.persistence.PostmanTenant;
import com.postman.fiserv.mockserver.model.persistence.ResourceStatus;

@Service
public class PostmanResourceService {

    private final PostmanTenantRepository tenantRepository;
    private final PostmanResourceRepository resourceRepository;

    public PostmanResourceService(
            PostmanTenantRepository tenantRepository,
            PostmanResourceRepository resourceRepository) {
        this.tenantRepository = tenantRepository;
        this.resourceRepository = resourceRepository;
    }

    public Optional<PostmanTenant> findTenant(String tenantName) {
        return tenantRepository.findByTenantName(tenantName);
    }

    public PostmanTenant createTenant(String tenantName, String postmanWorkspaceId) {
        Instant now = Instant.now();
        PostmanTenant tenant = new PostmanTenant();
        tenant.setTenantName(tenantName);
        tenant.setPostmanWorkspaceId(postmanWorkspaceId);
        tenant.setStatus(ResourceStatus.READY);
        tenant.setCreatedAt(now);
        tenant.setUpdatedAt(now);
        return tenantRepository.save(tenant);
    }

    public void deleteTenant(String tenantName) {
        tenantRepository.deleteByTenantName(tenantName);
    }

    public Optional<PostmanResource> findResource(String tenantName, String branchName, String filePath) {
        return resourceRepository.findByTenantNameAndBranchNameAndFilePath(tenantName, branchName, filePath);
    }

    public boolean tenantHasAnyResource(String tenantName) {
        return resourceRepository.countByTenantName(tenantName) > 0;
    }

    /**
     * Persist a fresh resource doc in PROVISIONING state with only path metadata populated.
     * Postman IDs get filled in by subsequent {@link #save(PostmanResource)} calls as each
     * Postman API step succeeds — so a mid-flow crash leaves enough state to resume on retry.
     */
    public PostmanResource startProvisioning(String tenantName, String branchName, SpecFileInfo info) {
        Instant now = Instant.now();
        PostmanResource resource = new PostmanResource();
        resource.setTenantName(tenantName);
        resource.setBranchName(branchName);
        resource.setFilePath(info.filePath());
        resource.setVersion(info.version());
        resource.setFileName(info.fileName());
        resource.setStatus(ResourceStatus.PROVISIONING);
        resource.setCreatedAt(now);
        resource.setUpdatedAt(now);
        return resourceRepository.save(resource);
    }

    public PostmanResource save(PostmanResource resource) {
        resource.setUpdatedAt(Instant.now());
        return resourceRepository.save(resource);
    }

    public PostmanResource markResourceStatus(PostmanResource resource, ResourceStatus status, String errorMessage) {
        resource.setStatus(status);
        resource.setErrorMessage(errorMessage);
        resource.setUpdatedAt(Instant.now());
        return resourceRepository.save(resource);
    }

    public void deleteResource(PostmanResource resource) {
        resourceRepository.delete(resource);
    }
}
