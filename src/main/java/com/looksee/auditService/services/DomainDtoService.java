package com.looksee.auditService.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.auditService.models.Audit;
import com.looksee.auditService.models.AuditRecord;
import com.looksee.auditService.models.Domain;
import com.looksee.auditService.models.DomainAuditRecord;
import com.looksee.auditService.models.PageState;
import com.looksee.auditService.models.dto.DomainDto;
import com.looksee.auditService.models.enums.ExecutionStatus;
import com.looksee.auditService.models.repository.AuditRecordRepository;
import com.looksee.auditService.models.repository.AuditRepository;
import com.looksee.auditService.models.repository.PageStateRepository;
import com.looksee.utils.AuditUtils;

@Service
public class DomainDtoService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(DomainDtoService.class.getName());
	
	@Autowired
	private AuditRecordRepository audit_record_repo;
	
	@Autowired
	private AuditRepository audit_repo;
	
	@Autowired
	private PageStateRepository page_state_repo;
	
	/**
	 * 
	 * @param domain
	 * @return
	 * 
	 * @pre domain != null
	 */
	public DomainDto build(Domain domain) {
		assert domain != null;
		
		Optional<AuditRecord> audit_record_opt = audit_record_repo.getMostRecentAuditRecordForDomain(domain.getId());

		int audited_pages = 0;
		int page_count = 0;
		
		if (!audit_record_opt.isPresent()) {
			return new DomainDto(domain.getId(), 
								 domain.getUrl(), 
								 0, 
								 0, 
								 0, 
								 1.0, 
								 0, 
								 1.0, 
								 0, 
								 1.0, 
								 0, 
								 1.0, 
								 false, 
								 1.0, 
								 "",
								 ExecutionStatus.COMPLETE);
		}
		
		// get most recent audit record for this domain
		DomainAuditRecord domain_audit = (DomainAuditRecord)audit_record_opt.get();

		// get all content audits for most recent audit record and calculate overall
		// score
		Set<Audit> content_audits = audit_repo.getAllContentAuditsForDomainRecord(domain_audit.getId());
		double content_score = AuditUtils.calculateScore(content_audits);

		// get all info architecture audits for most recent audit record and calculate
		// overall score
		Set<Audit> info_arch_audits = audit_repo
				.getAllInformationArchitectureAuditsForDomainRecord(domain_audit.getId());

		double info_arch_score = AuditUtils.calculateScore(info_arch_audits);

		// get all accessibility audits for most recent audit record and calculate
		// overall score
		Set<Audit> accessibility_audits = audit_repo.getAllAccessibilityAuditsForDomainRecord(domain_audit.getId());

		double accessibility_score = AuditUtils.calculateScore(accessibility_audits);

		// get all Aesthetic audits for most recent audit record and calculate overall
		// score
		Set<Audit> aesthetics_audits = audit_repo
				.getAllAestheticsAuditsForDomainRecord(domain_audit.getId());

		double aesthetics_score = AuditUtils.calculateScore(aesthetics_audits);

		// build domain stats
		// add domain stat to set

		// check if there is a current audit running
		Set<AuditRecord> page_audit_records = audit_record_repo.getAllPageAudits(domain_audit.getId());
		Set<PageState> page_states = page_state_repo.getPageStatesForDomainAuditRecord(domain_audit.getId());
		Map<String, Boolean> page_urls = new HashMap<>();
		
		for(PageState page : page_states) {
			page_urls.put(page.getUrl(), Boolean.TRUE);
		}
		page_count = page_urls.size();

		double content_progress = 0.0;
		double aesthetic_progress = 0.0;
		double info_architecture_progress = 0.0;
		boolean is_audit_running = false;
		double data_extraction_progress = 0.0;

		/*
		content_progress += domain_audit.getContentAuditProgress();
		aesthetic_progress += domain_audit.getAestheticAuditProgress();
		info_architecture_progress += domain_audit.getInfoArchitechtureAuditProgress();
		 */
		data_extraction_progress = domain_audit.getDataExtractionProgress();
		for (AuditRecord record : page_audit_records) {
			content_progress += record.getContentAuditProgress();
			aesthetic_progress += record.getAestheticAuditProgress();
			info_architecture_progress += record.getInfoArchitechtureAuditProgress();
			//data_extraction_progress += record.getDataExtractionProgress();

			if (record.isComplete()) {
				audited_pages++;
			}
			else {
				is_audit_running = true;
			}
		}

		
		if (page_audit_records.size() > 0) {
			content_progress = content_progress / page_count;
			info_architecture_progress = info_architecture_progress / page_count;
			aesthetic_progress = aesthetic_progress / page_count;
			//data_extraction_progress = (data_extraction_progress / page_count);
		}
		
		ExecutionStatus status = domain_audit.getStatus();
		/*
		if(1.0 == content_progress && 1.0 == info_architecture_progress && 1.0 == aesthetic_progress && 1.0 == data_extraction_progress) {
			status = ExecutionStatus.COMPLETE;
		}
		else {
			status = ExecutionStatus.IN_PROGRESS;
		}
		*/

		
		return new DomainDto(domain.getId(), 
							  domain.getUrl(), 
							  page_count, 
							  audited_pages, 
							  content_score,
							  content_progress, 
							  info_arch_score, 
							  info_architecture_progress, 
							  accessibility_score, 
							  100.0,
							  aesthetics_score, 
							  aesthetic_progress, 
							  is_audit_running, 
							  data_extraction_progress,
							  domain_audit.getStatusMessage(),
							  status);
	}
}
