package com.looksee.auditService.models.message;

import com.looksee.auditService.models.enums.BrowserType;
import com.looksee.auditService.models.journeys.Journey;

import lombok.Getter;
import lombok.Setter;

public class DiscardedJourneyMessage extends DomainAuditMessage {

	@Getter
	@Setter
	private Journey journey;
	
	@Getter
	@Setter
	private BrowserType browserType;

	@Getter
	@Setter
	private long auditRecordId;
   
	public DiscardedJourneyMessage() {}
	
	public DiscardedJourneyMessage(Journey journey, 
								   BrowserType browserType, 
								   long accountId, 
								   long auditRecordId) {
		super(accountId, auditRecordId);
		setJourney(journey);
		setBrowserType(browserType);
		setAuditRecordId(auditRecordId);
	}
}
