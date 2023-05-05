package com.looksee.auditService.models.dto;

import com.looksee.auditService.models.enums.AuditLevel;

public class AuditUpdateDto {
	
	private long auditRecordId;
	private AuditLevel auditType;
	private double dataExtractionProgress;
	private double aestheticAuditProgress; 
	private double contentAuditProgress; 
	private double infoArchitechtureAuditProgress;
	private double overallProgress;
	private int completePages;
	private int totalPages;

	public AuditUpdateDto(long audit_record_id, 
						  AuditLevel audit_type, 
						  double data_extraction_progress,
						  double aesthetic_audit_progress, 
						  double content_audit_progress, 
						  double info_architechture_audit_progress,
						  double overall_progress, 
						  int complete_pages, 
						  int total_pages) 
	{
		setAuditRecordId(audit_record_id);
		setAuditType(audit_type);
		setDataExtractionProgress(data_extraction_progress);
		setAestheticAuditProgress(aesthetic_audit_progress);
		setContentAuditProgress(content_audit_progress);
		setInfoArchitechtureAuditProgress(info_architechture_audit_progress);
		setOverallProgress(overall_progress);
		setCompletePages(complete_pages);
		setTotalPages(total_pages);
	}

	public long getAuditRecordId() {
		return auditRecordId;
	}

	public void setAuditRecordId(long auditRecordId) {
		this.auditRecordId = auditRecordId;
	}

	public AuditLevel getAuditType() {
		return auditType;
	}

	public void setAuditType(AuditLevel auditType) {
		this.auditType = auditType;
	}

	public double getDataExtractionProgress() {
		return dataExtractionProgress;
	}

	public void setDataExtractionProgress(double dataExtractionProgress) {
		this.dataExtractionProgress = dataExtractionProgress;
	}

	public double getAestheticAuditProgress() {
		return aestheticAuditProgress;
	}

	public void setAestheticAuditProgress(double aestheticAuditProgress) {
		this.aestheticAuditProgress = aestheticAuditProgress;
	}

	public double getContentAuditProgress() {
		return contentAuditProgress;
	}

	public void setContentAuditProgress(double contentAuditProgress) {
		this.contentAuditProgress = contentAuditProgress;
	}

	public double getInfoArchitechtureAuditProgress() {
		return infoArchitechtureAuditProgress;
	}

	public void setInfoArchitechtureAuditProgress(double infoArchitechtureAuditProgress) {
		this.infoArchitechtureAuditProgress = infoArchitechtureAuditProgress;
	}

	public double getOverallProgress() {
		return overallProgress;
	}

	public void setOverallProgress(double overallProgress) {
		this.overallProgress = overallProgress;
	}

	public int getCompletePages() {
		return completePages;
	}

	public void setCompletePages(int completePages) {
		this.completePages = completePages;
	}

	public int getTotalPages() {
		return totalPages;
	}

	public void setTotalPages(int totalPages) {
		this.totalPages = totalPages;
	}
	
}
