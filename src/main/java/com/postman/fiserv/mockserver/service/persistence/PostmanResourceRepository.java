package com.postman.fiserv.mockserver.service.persistence;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.postman.fiserv.mockserver.model.persistence.PostmanResource;

public interface PostmanResourceRepository extends MongoRepository<PostmanResource, String> {
    Optional<PostmanResource> findByTenantNameAndBranchNameAndFilePath(
            String tenantName, String branchName, String filePath);
    long countByTenantName(String tenantName);
}
