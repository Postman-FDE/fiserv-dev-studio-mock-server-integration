package com.postman.fiserv.mockserver.model.postman;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpecResponse(String id, String name, String type) {}
