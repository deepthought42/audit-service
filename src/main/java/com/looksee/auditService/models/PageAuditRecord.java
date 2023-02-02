package com.looksee.auditService.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.neo4j.core.schema.Relationship;

import com.looksee.auditService.models.enums.AuditLevel;
import com.looksee.auditService.models.enums.ExecutionStatus;


/**
 * Record detailing an set of {@link Audit audits}.
 */
public class PageAuditRecord extends AuditRecord {
	
	private double contentAuditProgress;
	private String contentAuditMsg;
	
	private double infoArchitectureAuditProgress;
	private String infoArchMsg;
	
	private double aestheticAuditProgress;
	private String aestheticMsg;

	
	@Relationship(type = "HAS")
	private Set<Audit> audits;
	
	private String url;
	private long elements_found;
	private long elements_reviewed;
	
	public PageAuditRecord() {
		setAudits(new HashSet<>());
		setKey(generateKey());
		setContentAuditProgress(0.0);
		setContentAuditMsg("");
		setInfoArchitectureAuditProgress(0.0);
		setInfoArchMsg("");
		setAestheticAuditProgress(0.0);
		setAestheticMsg("");
		setKey(generateKey());
	}
	
	/**
	 * Constructor 
	 * 
	 * @param url
	 * @param status
	 */
	public PageAuditRecord(String url, ExecutionStatus status) {
		super(status);
		setStatus(status);
		setUrl(url);
		setContentAuditProgress(0.0);
		setContentAuditMsg("");
		setInfoArchitectureAuditProgress(0.0);
		setInfoArchMsg("");
		setAestheticAuditProgress(0.0);
		setAestheticMsg("");
		setKey(generateKey());
	}
	
	/**
	 * Constructor
	 * @param audits TODO
	 * @param page_state TODO
	 * @param is_part_of_domain_audit TODO
	 * @param audit_stats {@link AuditStats} object with statics for audit progress
	 * @pre audits != null
	 * @pre page_state != null
	 * @pre status != null;
	 */
	public PageAuditRecord(
			ExecutionStatus status,
			boolean is_part_of_domain_audit
	) {
		assert status != null;
		
		setAudits(audits);
		setStatus(status);
		setLevel( AuditLevel.PAGE);
		setKey(generateKey());
		setContentAuditProgress(0.0);
		setContentAuditMsg("");
		setInfoArchitectureAuditProgress(0.0);
		setInfoArchMsg("");
		setAestheticAuditProgress(0.0);
		setAestheticMsg("");
		setKey(generateKey());
	}
	
	/**
	 * Constructor
	 * @param level TODO
	 * 
	 */
	public PageAuditRecord(long id, 
					   ExecutionStatus status, 
					   AuditLevel level, 
					   String key, 
					   LocalDateTime startTime,
					   double aestheticAuditProgress, 
					   String aestheticMsg, 
					   String contentAuditMsg, 
					   double contentAuditProgress,
					   String infoArchMsg, 
					   double infoArchAuditProgress,
					   LocalDateTime created_at, 
					   LocalDateTime endTime
	) {
		super(id, status, level, key, startTime, created_at, endTime);
		setAestheticAuditProgress(aestheticAuditProgress);
		setAestheticMsg(aestheticMsg);
		setContentAuditMsg(contentAuditMsg);
		setContentAuditProgress(contentAuditProgress);
		setInfoArchMsg(infoArchMsg);
		setInfoArchitectureAuditProgress(infoArchAuditProgress);
		setColors(new ArrayList<String>());
	}

	public boolean isComplete() {
		return (this.getAestheticAuditProgress() >= 1.0
				&& this.getContentAuditProgress() >= 1.0
				&& this.getInfoArchitechtureAuditProgress() >= 1.0);
	}
	
	public String generateKey() {
		return "pageauditrecord:"+org.apache.commons.codec.digest.DigestUtils.sha256Hex( System.currentTimeMillis() + " " );
	}

	public Set<Audit> getAudits() {
		return audits;
	}

	public void setAudits(Set<Audit> audits) {
		this.audits = audits;
	}

	public void addAudit(Audit audit) {
		this.audits.add( audit );
	}
	
	public void addAudits(Set<Audit> audits) {
		this.audits.addAll( audits );
	}

	public long getElementsFound() {
		return elements_found;
	}

	public void setElementsFound(long elements_found) {
		this.elements_found = elements_found;
	}

	public long getElementsReviewed() {
		return elements_reviewed;
	}

	public void setElementsReviewed(long elements_reviewed) {
		this.elements_reviewed = elements_reviewed;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	
	public double getContentAuditProgress() {
		return contentAuditProgress;
	}

	public void setContentAuditProgress(double content_audit_progress) {
		this.contentAuditProgress = content_audit_progress;
	}

	public double getInfoArchitechtureAuditProgress() {
		return infoArchitectureAuditProgress;
	}

	public void setInfoArchitectureAuditProgress(double info_arch_audit_progress) {
		this.infoArchitectureAuditProgress = info_arch_audit_progress;
	}

	public double getAestheticAuditProgress() {
		return aestheticAuditProgress;
	}

	public void setAestheticAuditProgress(double aesthetic_audit_progress) {
		this.aestheticAuditProgress = aesthetic_audit_progress;
	}

	public String getContentAuditMsg() {
		return contentAuditMsg;
	}

	public void setContentAuditMsg(String content_audit_msg) {
		this.contentAuditMsg = content_audit_msg;
	}
	
	public String getInfoArchMsg() {
		return infoArchMsg;
	}

	public void setInfoArchMsg(String info_arch_msg) {
		this.infoArchMsg = info_arch_msg;
	}

	public String getAestheticMsg() {
		return aestheticMsg;
	}

	public void setAestheticMsg(String aesthetic_msg) {
		this.aestheticMsg = aesthetic_msg;
	}

}
