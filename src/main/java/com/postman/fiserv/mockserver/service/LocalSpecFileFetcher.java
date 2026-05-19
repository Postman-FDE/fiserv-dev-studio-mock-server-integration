package com.postman.fiserv.mockserver.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Component;

import com.postman.fiserv.mockserver.config.SpecsProperties;

@Component
public class LocalSpecFileFetcher implements SpecFileFetcher {

    private final SpecsProperties properties;

    public LocalSpecFileFetcher(SpecsProperties properties) {
        this.properties = properties;
    }

    @Override
    public String fetch(String webhookPath) {
        Path file = Paths.get(properties.baseDir(), webhookPath);
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            throw new SpecNotFoundException(
                    "Spec file not found at " + file.toAbsolutePath(), e);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read spec file " + file.toAbsolutePath(), e);
        }
    }
}
