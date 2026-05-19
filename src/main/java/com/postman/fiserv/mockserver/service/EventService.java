package com.postman.fiserv.mockserver.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.postman.fiserv.mockserver.model.EventServiceRequest;
import com.postman.fiserv.mockserver.model.EventServiceResponse;
import com.postman.fiserv.mockserver.model.FileOperationResult;
import com.postman.fiserv.mockserver.model.OperationType;
import com.postman.fiserv.mockserver.model.SpecFileInfo;
import com.postman.fiserv.mockserver.model.persistence.PostmanResource;
import com.postman.fiserv.mockserver.service.persistence.PostmanResourceService;

@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final SpecFileFetcher specFetcher;
    private final SpecParser specParser;
    private final PostmanService postmanService;
    private final PostmanResourceService resourceService;

    public EventService(
            SpecFileFetcher specFetcher,
            SpecParser specParser,
            PostmanService postmanService,
            PostmanResourceService resourceService) {
        this.specFetcher = specFetcher;
        this.specParser = specParser;
        this.postmanService = postmanService;
        this.resourceService = resourceService;
    }

    public EventServiceResponse process(EventServiceRequest request) {
        log.info("Processing event for tenant='{}' branch='{}' (added={}, modified={}, removed={})",
                request.tenantName(), request.branchName(),
                request.added().size(), request.modified().size(), request.removed().size());

        List<FileOperationResult> results = new ArrayList<>();

        for (String filePath : request.added()) {
            results.add(processAdded(request.tenantName(), request.branchName(), filePath));
        }
        for (String filePath : request.modified()) {
            results.add(processModified(request.tenantName(), request.branchName(), filePath));
        }
        for (String filePath : request.removed()) {
            results.add(processRemoved(request.tenantName(), request.branchName(), filePath));
        }

        if (!request.removed().isEmpty()) {
            try {
                postmanService.cleanupTenantIfEmpty(request.tenantName());
            } catch (RuntimeException e) {
                log.error("Tenant cleanup failed for '{}'", request.tenantName(), e);
            }
        }

        return new EventServiceResponse(results);
    }

    private FileOperationResult processAdded(String tenantName, String branchName, String filePath) {
        try {
            SpecFileInfo info = loadSpec(filePath);
            PostmanResource resource = postmanService.handleAdded(tenantName, branchName, info);
            return FileOperationResult.ok(filePath, OperationType.ADDED, resource.getPostmanMockServerUrl());
        } catch (RuntimeException e) {
            log.error("ADDED failed for {} (tenant={}, branch={})", filePath, tenantName, branchName, e);
            return FileOperationResult.failed(filePath, OperationType.ADDED, e.getMessage());
        }
    }

    private FileOperationResult processModified(String tenantName, String branchName, String filePath) {
        try {
            Optional<PostmanResource> existing = resourceService.findResource(tenantName, branchName, filePath);
            if (existing.isEmpty()) {
                return FileOperationResult.failed(filePath, OperationType.MODIFIED,
                        "No existing resource found for tenant=" + tenantName + " branch=" + branchName);
            }
            SpecFileInfo info = loadSpec(filePath);
            PostmanResource updated = postmanService.handleModified(existing.get(), info);
            return FileOperationResult.ok(filePath, OperationType.MODIFIED, updated.getPostmanMockServerUrl());
        } catch (RuntimeException e) {
            log.error("MODIFIED failed for {} (tenant={}, branch={})", filePath, tenantName, branchName, e);
            return FileOperationResult.failed(filePath, OperationType.MODIFIED, e.getMessage());
        }
    }

    private FileOperationResult processRemoved(String tenantName, String branchName, String filePath) {
        try {
            Optional<PostmanResource> existing = resourceService.findResource(tenantName, branchName, filePath);
            if (existing.isEmpty()) {
                return FileOperationResult.failed(filePath, OperationType.REMOVED,
                        "No existing resource found for tenant=" + tenantName + " branch=" + branchName);
            }
            postmanService.handleRemoved(existing.get());
            return FileOperationResult.ok(filePath, OperationType.REMOVED, "deleted");
        } catch (RuntimeException e) {
            log.error("REMOVED failed for {} (tenant={}, branch={})", filePath, tenantName, branchName, e);
            return FileOperationResult.failed(filePath, OperationType.REMOVED, e.getMessage());
        }
    }

    private SpecFileInfo loadSpec(String filePath) {
        String rawYaml = specFetcher.fetch(filePath);
        return specParser.parse(filePath, rawYaml);
    }
}
