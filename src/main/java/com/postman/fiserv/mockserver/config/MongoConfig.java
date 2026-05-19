package com.postman.fiserv.mockserver.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import com.postman.fiserv.mockserver.model.persistence.ResourceStatus;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                new ResourceStatusToStringConverter(),
                new StringToResourceStatusConverter()
        ));
    }

    @WritingConverter
    static class ResourceStatusToStringConverter implements Converter<ResourceStatus, String> {
        @Override
        public String convert(ResourceStatus source) {
            return source.name().toLowerCase();
        }
    }

    @ReadingConverter
    static class StringToResourceStatusConverter implements Converter<String, ResourceStatus> {
        @Override
        public ResourceStatus convert(String source) {
            return ResourceStatus.valueOf(source.toUpperCase());
        }
    }
}
