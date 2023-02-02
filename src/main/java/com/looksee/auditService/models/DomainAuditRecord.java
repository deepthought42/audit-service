package com.looksee.auditService.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.neo4j.core.schema.Relationship;

import com.looksee.auditService.models.enums.AuditLevel;
import com.looksee.auditService.models.enums.ExecutionStatus;
import com.looksee.auditService.models.enums.JourneyStatus;


/**
 * Record detailing an set of {@link Audit audits}.
 */
public class DomainAuditRecord extends AuditRecord {
	
	private double dataExtractionProgress;
	private String dataExtractionMsg;
	
	private int total_pages;
	private Map<String, JourneyStatus> journey_status_map;
	
	@Relationship(type = "HAS")
	private Set<PageAuditRecord> page_audit_records;
	
	public DomainAuditRecord() {
		super();
		setAudits(new HashSet<>());
	}
	
	/**
	 * Constructor
	 * 
	 * @param audit_stats {@link AuditStats} object with statics for audit progress
	 * @param level TODO
	 * 
	 * @pre audit_stats != null;
	 */
	public DomainAuditRecord(ExecutionStatus status) {
		super(status);
		assert status != null;
		
		setAudits(new HashSet<>());
		setLevel( AuditLevel.DOMAIN);
		setStartTime(LocalDateTime.now());
		setTotalPages(0);
		setDataExtractionProgress(0.0);
		setDataExtractionMsg("");
		setKey(generateKey());
	}

	/**
	 * Constructor
	 * @param level TODO
	 * 
	 */
	public DomainAuditRecord(long id, 
					   ExecutionStatus status, 
					   AuditLevel level, 
					   String key, 
					   LocalDateTime startTime,
					   String dataExtractionMsg, 
					   double dataExtractionProgress,
					   LocalDateTime created_at, 
					   LocalDateTime endTime
	) {
		super(id, status, level, key, startTime, created_at, endTime);
		setDataExtractionMsg(dataExtractionMsg);
		setDataExtractionProgress(dataExtractionProgress);
		setColors(new ArrayList<String>());
	}

	
	public String generateKey() {
		return "domainauditrecord:"+UUID.randomUUID().toString()+org.apache.commons.codec.digest.DigestUtils.sha256Hex(System.currentTimeMillis() + "");
	}

	public Set<PageAuditRecord> getAudits() {
		return page_audit_records;
	}

	public void setAudits(Set<PageAuditRecord> audits) {
		this.page_audit_records = audits;
	}

	public void addAudit(PageAuditRecord audit) {
		this.page_audit_records.add( audit );
	}
	
	public void addAudits(Set<PageAuditRecord> audits) {
		this.page_audit_records.addAll( audits );
	}

	public int getTotalPages() {
		return total_pages;
	}

	public void setTotalPages(int total_pages) {
		this.total_pages = total_pages;
	}

	public Map<String, JourneyStatus> getJourneyStatusMap() {
		return journey_status_map;
	}

	public void setJourneyStatusMap(Map<String, JourneyStatus> journey_status_map) {
		this.journey_status_map = journey_status_map;
	}
	

	public double getDataExtractionProgress() {
		return dataExtractionProgress;
	}

	public void setDataExtractionProgress(double data_extraction_progress) {
		this.dataExtractionProgress = data_extraction_progress;
	}

	public String getDataExtractionMsg() {
		return dataExtractionMsg;
	}

	public void setDataExtractionMsg(String data_extraction_msg) {
		this.dataExtractionMsg = data_extraction_msg;
	}
}
