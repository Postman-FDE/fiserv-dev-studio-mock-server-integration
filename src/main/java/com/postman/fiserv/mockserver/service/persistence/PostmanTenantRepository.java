package com.postman.fiserv.mockserver.service.persistence;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.postman.fiserv.mockserver.model.persistence.PostmanTenant;

public interface PostmanTenantRepository extends MongoRepository<PostmanTenant, String> {
    Optional<PostmanTenant> findByTenantName(String tenantName);
    void deleteByTenantName(String tenantName);
}
