package com.postman.fiserv.mockserver.model.postman;

public record CreateMockRequest(Mock mock) {
    public record Mock(String name, String collection, Boolean isPrivate) {
        // Jackson serializes the record property name verbatim; the Postman API expects the
        // JSON field to be "private", which is a Java reserved word. Use a tiny shim.
        @com.fasterxml.jackson.annotation.JsonProperty("private")
        public Boolean isPrivate() {
            return isPrivate;
        }
    }
}
