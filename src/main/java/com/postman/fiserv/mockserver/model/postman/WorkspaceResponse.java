package com.postman.fiserv.mockserver.model.postman;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkspaceResponse(Workspace workspace) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Workspace(String id, String name) {}
}
