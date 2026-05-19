package com.postman.fiserv.mockserver.model.postman;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MockResponse(Mock mock) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Mock(String id, String mockUrl, String name) {}
}
