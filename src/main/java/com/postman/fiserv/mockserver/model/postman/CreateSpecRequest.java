package com.postman.fiserv.mockserver.model.postman;

import java.util.List;

public record CreateSpecRequest(String name, String type, List<SpecFile> files) {
    public record SpecFile(String path, String content) {}
}
