package com.postman.fiserv.mockserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "specs.local")
public record SpecsProperties(String baseDir) {}
