package com.postman.fiserv.mockserver.model.postman;

public record CreateWorkspaceRequest(Workspace workspace) {
    public record Workspace(String name, String type, String description) {}
}
