package com.postman.fiserv.mockserver.model;

public record SpecFileInfo(
        String filePath,
        String version,
        String fileName,
        String pathFromVersion,
        String openapiType,
        String rawYaml
) {}
