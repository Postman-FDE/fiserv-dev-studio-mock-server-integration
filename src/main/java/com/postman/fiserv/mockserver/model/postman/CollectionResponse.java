package com.postman.fiserv.mockserver.model.postman;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CollectionResponse(Collection collection) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Collection(Info info, List<Variable> variable) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Info(
                @JsonProperty("_postman_id") String postmanId,
                String name
        ) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Variable(String key, String value) {}
    }
}
