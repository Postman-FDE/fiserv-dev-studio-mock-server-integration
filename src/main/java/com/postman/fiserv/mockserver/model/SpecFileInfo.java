package com.postman.fiserv.mockserver.model;

public record SpecFileInfo(
        String filePath,
        String fileName,
        String pathFromReference,
        String openapiType,
        String rawYaml
) {}
