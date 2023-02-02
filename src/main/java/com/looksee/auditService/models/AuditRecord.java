package com.looksee.auditService.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.neo4j.core.schema.Node;

import com.looksee.auditService.models.enums.AuditLevel;
import com.looksee.auditService.models.enums.ExecutionStatus;


/**
 * Record detailing an set of {@link Audit audits}.
 */
@Node
public class AuditRecord extends LookseeObject {	
	private String status;
	private String statusMessage;
	private String level;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	

	private String targetUserAge;
	private String targetUserEducation;

	//DESIGN SYSTEM VALUES
	private List<String> colors;
	
	public AuditRecord() {
		setStartTime(LocalDateTime.now());
		setStatus(ExecutionStatus.UNKNOWN);
		setStatusMessage("");
		setLevel(AuditLevel.UNKNOWN);
		
		setColors(new ArrayList<String>());
	}
	
	public AuditRecord(ExecutionStatus status) {
		setStartTime(LocalDateTime.now());
		setStatus(status);
		setStatusMessage("");
		setLevel(AuditLevel.UNKNOWN);
		
		setColors(new ArrayList<String>());
	}
	
	/**
	 * Constructor
	 * @param level TODO
	 * 
	 */
	public AuditRecord(long id, 
					   ExecutionStatus status, 
					   AuditLevel level, 
					   String key, 
					   LocalDateTime startTime,
					   LocalDateTime created_at, 
					   LocalDateTime endTime
	) {
		setId(id);
		setStatus(status);
		setLevel(level);
		setKey(key);
		setStartTime(endTime);
		setCreatedAt(created_at);
		setEndTime(endTime);
		setColors(new ArrayList<String>());
	}

	public String generateKey() {
		return "auditrecord:" + UUID.randomUUID().toString() + org.apache.commons.codec.digest.DigestUtils.sha256Hex(System.currentTimeMillis() + "");
	}

	public ExecutionStatus getStatus() {
		return ExecutionStatus.create(status);
	}

	public void setStatus(ExecutionStatus status) {
		this.status = status.getShortName();
	}

	public AuditLevel getLevel() {
		return AuditLevel.create(level);
	}

	public void setLevel(AuditLevel level) {
		this.level = level.toString();
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime start_time) {
		this.startTime = start_time;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalDateTime end_time) {
		this.endTime = end_time;
	}

	public String getTargetUserAge() {
		return targetUserAge;
	}

	public void setTargetUserAge(String target_user_age) {
		this.targetUserAge = target_user_age;
	}

	public String getTargetUserEducation() {
		return targetUserEducation;
	}

	public void setTargetUserEducation(String target_user_education) {
		this.targetUserEducation = target_user_education;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(String status_message) {
		this.statusMessage = status_message;
	}
	
	@Override
	public String toString() {
		return this.getId()+", "+this.getKey()+", "+this.getStatus()+", "+this.getStatusMessage();
	}
	
	@Override
	public AuditRecord clone() {
		return new AuditRecord(getId(),
							   getStatus(),
							   getLevel(),
							   getKey(),
							   getStartTime(),
							   getCreatedAt(), 
							   getEndTime());
	}

	public List<String> getColors() {
		return colors;
	}

	public void setColors(List<String> colors) {
		this.colors = colors;
	}
	
	public boolean addColor(String color){
		if(!getColors().contains(color)) {
			return getColors().add(color);
		}
		
		return true;	
	}
}
