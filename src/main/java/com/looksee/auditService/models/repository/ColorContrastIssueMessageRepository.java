package com.looksee.auditService.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import com.looksee.auditService.models.ColorContrastIssueMessage;

import io.github.resilience4j.retry.annotation.Retry;


@Repository
@Retry(name="neoforj")
public interface ColorContrastIssueMessageRepository extends Neo4jRepository<ColorContrastIssueMessage, Long>  {
	
}
