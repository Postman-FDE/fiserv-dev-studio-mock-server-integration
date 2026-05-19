package com.postman.fiserv.mockserver.service;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import com.postman.fiserv.mockserver.model.SpecFileInfo;

@Component
public class SpecParser {

    private static final String PATH_PREFIX = "reference/";

    public SpecFileInfo parse(String filePath, String rawYaml) {
        Map<String, Object> root = parseYamlRoot(rawYaml);
        String openapiType = detectOpenapiType(root);

        if (!filePath.startsWith(PATH_PREFIX)) {
            throw new IllegalArgumentException(
                    "File path must start with 'reference/<version>/…': " + filePath);
        }
        String pathFromVersion = filePath.substring(PATH_PREFIX.length());
        String[] remainingSegments = pathFromVersion.split("/");
        if (remainingSegments.length < 2 || remainingSegments[0].isBlank()) {
            throw new IllegalArgumentException(
                    "File path must include version and file segments: " + filePath);
        }
        String version = remainingSegments[0];
        String fileName = remainingSegments[remainingSegments.length - 1];

        return new SpecFileInfo(filePath, version, fileName, pathFromVersion, openapiType, rawYaml);
    }

    private Map<String, Object> parseYamlRoot(String rawYaml) {
        Yaml yaml = new Yaml();
        Object loaded = yaml.load(rawYaml);
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Spec root is not a YAML mapping");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> typed = (Map<String, Object>) map;
        return typed;
    }

    private String detectOpenapiType(Map<String, Object> root) {
        Object openapi = root.get("openapi");
        if (openapi != null) {
            String value = openapi.toString();
            if (value.startsWith("3.1")) return "OPENAPI:3.1";
            if (value.startsWith("3.0")) return "OPENAPI:3.0";
        }
        Object swagger = root.get("swagger");
        if (swagger != null && swagger.toString().startsWith("2.")) {
            return "OPENAPI:2.0";
        }
        throw new IllegalArgumentException(
                "Could not detect OpenAPI version; missing or unsupported 'openapi' / 'swagger' field");
    }
}
