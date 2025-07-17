package com.looksee.auditService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;


@SpringBootApplication
@ComponentScan(basePackages = {
    "com.looksee.auditService",
    "com.looksee.services",
    "com.looksee.models",
    "com.looksee.gcp",
    "com.looksee.utils",
    "com.looksee.config"  // Include config package from core JAR
})
@EnableNeo4jRepositories(basePackages = "com.looksee.models.repository")
@PropertySources({
	@PropertySource("classpath:application.properties")
})
public class Application {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public static void main(String[] args)  {
		SpringApplication.run(Application.class, args);
	}
}