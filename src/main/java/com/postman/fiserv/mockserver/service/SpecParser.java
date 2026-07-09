package com.postman.fiserv.mockserver.service;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import com.postman.fiserv.mockserver.model.SpecFileInfo;

@Component
public class SpecParser {

    private static final String PATH_PREFIX = "reference/";

    // SnakeYAML defaults codePointLimit to 3 MB, which rejects larger specs during load()
    // with "The incoming YAML document exceeds the limit: 3145728 code points." Raise it so
    // big specs parse locally; Postman's own create-spec size limit (if any) is enforced later.
    private static final int MAX_YAML_CODE_POINTS = 20 * 1024 * 1024;

    public SpecFileInfo parse(String filePath, String rawYaml) {
        Map<String, Object> root = parseYamlRoot(rawYaml);
        String openapiType = detectOpenapiType(root);

        // Real webhook events arrive as "reference/…"; POC fixtures organized under
        // specs/<tenant>/reference/… carry the tenant directory as a prefix. Either is
        // fine — locate 'reference/' as a whole segment and take everything after as the
        // canonical resource path. The original filePath is preserved on the result so
        // the fetcher still reads from the right place on disk.
        int referenceIdx;
        if (filePath.startsWith(PATH_PREFIX)) {
            referenceIdx = 0;
        } else {
            int slashRefIdx = filePath.indexOf("/" + PATH_PREFIX);
            if (slashRefIdx < 0) {
                throw new IllegalArgumentException(
                        "File path must contain a 'reference/' segment: " + filePath);
            }
            referenceIdx = slashRefIdx + 1;
        }
        String pathFromReference = filePath.substring(referenceIdx + PATH_PREFIX.length());
        if (pathFromReference.isBlank()) {
            throw new IllegalArgumentException(
                    "File path must include a file after 'reference/': " + filePath);
        }
        String[] segments = pathFromReference.split("/");
        String fileName = segments[segments.length - 1];

        return new SpecFileInfo(filePath, fileName, pathFromReference, openapiType, rawYaml);
    }

    private Map<String, Object> parseYamlRoot(String rawYaml) {
        LoaderOptions options = new LoaderOptions();
        options.setCodePointLimit(MAX_YAML_CODE_POINTS);
        Yaml yaml = new Yaml(options);
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
