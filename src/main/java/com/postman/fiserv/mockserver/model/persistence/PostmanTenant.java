package com.postman.fiserv.mockserver.model.persistence;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("postman_tenants")
public class PostmanTenant {

    @Id
    private String id;

    @Indexed(unique = true)
    private String tenantName;

    private String postmanWorkspaceId;
    private ResourceStatus status;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;

    public PostmanTenant() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getPostmanWorkspaceId() { return postmanWorkspaceId; }
    public void setPostmanWorkspaceId(String postmanWorkspaceId) { this.postmanWorkspaceId = postmanWorkspaceId; }

    public ResourceStatus getStatus() { return status; }
    public void setStatus(ResourceStatus status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
