package com.postman.fiserv.mockserver.model.persistence;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("postman_resources")
@CompoundIndex(
        name = "uniq_tenant_branch_filepath",
        def = "{'tenantName': 1, 'branchName': 1, 'filePath': 1}",
        unique = true
)
public class PostmanResource {

    @Id
    private String id;

    private String tenantName;
    private String branchName;
    private String filePath;

    private String version;
    private String fileName;

    private String postmanSpecId;
    private String postmanCollectionId;
    private String postmanCollectionUid;
    private String postmanMockServerId;
    private String postmanMockServerUrl;

    private ResourceStatus status;
    private String errorMessage;

    private Instant createdAt;
    private Instant updatedAt;

    public PostmanResource() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getPostmanSpecId() { return postmanSpecId; }
    public void setPostmanSpecId(String postmanSpecId) { this.postmanSpecId = postmanSpecId; }

    public String getPostmanCollectionId() { return postmanCollectionId; }
    public void setPostmanCollectionId(String postmanCollectionId) { this.postmanCollectionId = postmanCollectionId; }

    public String getPostmanCollectionUid() { return postmanCollectionUid; }
    public void setPostmanCollectionUid(String postmanCollectionUid) { this.postmanCollectionUid = postmanCollectionUid; }

    public String getPostmanMockServerId() { return postmanMockServerId; }
    public void setPostmanMockServerId(String postmanMockServerId) { this.postmanMockServerId = postmanMockServerId; }

    public String getPostmanMockServerUrl() { return postmanMockServerUrl; }
    public void setPostmanMockServerUrl(String postmanMockServerUrl) { this.postmanMockServerUrl = postmanMockServerUrl; }

    public ResourceStatus getStatus() { return status; }
    public void setStatus(ResourceStatus status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
