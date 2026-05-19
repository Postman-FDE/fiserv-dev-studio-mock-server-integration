package com.postman.fiserv.mockserver.service;

public class SpecNotFoundException extends RuntimeException {
    public SpecNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
