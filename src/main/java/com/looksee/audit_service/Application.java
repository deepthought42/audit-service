package com.looksee.audit_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.looksee*"})
@PropertySources({
	@PropertySource("classpath:application.properties")
})
@EnableNeo4jRepositories("com.looksee.audit_service.models.repository")
@EntityScan(basePackages = { "com.looksee.audit_service.models"} )
public class Application {

	public static void main(String[] args)  {
		SpringApplication.run(Application.class, args);
	}

}
