package com.postman.fiserv.mockserver.model;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record EventServiceRequest(
        @NotBlank String tenantName,
        @NotBlank String branchName,
        List<String> added,
        List<String> modified,
        List<String> removed
) {
    public EventServiceRequest {
        added = added == null ? List.of() : List.copyOf(added);
        modified = modified == null ? List.of() : List.copyOf(modified);
        removed = removed == null ? List.of() : List.copyOf(removed);
    }
}
