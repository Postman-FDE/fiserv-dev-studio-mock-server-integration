package com.postman.fiserv.mockserver.model.postman;

import java.util.Map;

public record GenerateCollectionRequest(String name, Map<String, Object> options) {}
