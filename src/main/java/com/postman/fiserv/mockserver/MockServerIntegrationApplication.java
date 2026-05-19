package com.postman.fiserv.mockserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MockServerIntegrationApplication {

	public static void main(String[] args) {
		SpringApplication.run(MockServerIntegrationApplication.class, args);
	}

}
