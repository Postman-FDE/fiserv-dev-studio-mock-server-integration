package com.postman.fiserv.mockserver.model.postman;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CollectionResponse(Collection collection) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Collection(Info info) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Info(
                @JsonProperty("_postman_id") String postmanId,
                String name
        ) {}
    }
}
