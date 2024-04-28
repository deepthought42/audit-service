package com.looksee.auditService.models.message;

import com.looksee.auditService.models.enums.AuditCategory;

public class AuditError extends Message{
	private String error_message;
	private AuditCategory audit_category;
	private double progress;

	@Getter
	@Setter
	private long auditRecordId;
	
	public AuditError(long accountId, 
					  long auditRecordId, 
					  String error_message,
					  AuditCategory category, 
					  double progress, 
					  long domainId
	) {
		super(accountId);
		setErrorMessage(error_message);
		setAuditCategory(category);
		setProgress(progress);
		setAuditRecordId(auditRecordId);
	}
}
