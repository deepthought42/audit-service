package com.looksee.auditService.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.auditService.models.journeys.Journey;

import io.github.resilience4j.retry.annotation.Retry;

@Repository
@Retry(name = "neoforj")
public interface JourneyRepository extends Neo4jRepository<Journey, Long>  {
	
	public Journey findByKey(@Param("key") String key);

	@Query("MATCH (j:Journey) WITH j MATCH (s:Step) WHERE id(s)=$step_id AND id(j)=$journey_id MERGE (j)-[:HAS]->(s) RETURN j")
	public Journey addStep(@Param("journey_id") long journey_id, @Param("step_id") long id);
}

