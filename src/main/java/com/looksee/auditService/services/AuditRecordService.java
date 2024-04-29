package com.looksee.auditService.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.collections4.IterableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.auditService.models.Account;
import com.looksee.auditService.models.Audit;
import com.looksee.auditService.models.AuditRecord;
import com.looksee.auditService.models.DesignSystem;
import com.looksee.auditService.models.DomainAuditRecord;
import com.looksee.auditService.models.Label;
import com.looksee.auditService.models.PageAuditRecord;
import com.looksee.auditService.models.PageState;
import com.looksee.auditService.models.UXIssueMessage;
import com.looksee.auditService.models.enums.AuditCategory;
import com.looksee.auditService.models.enums.ExecutionStatus;
import com.looksee.auditService.models.enums.JourneyStatus;
import com.looksee.auditService.models.repository.AccountRepository;
import com.looksee.auditService.models.repository.AuditRecordRepository;
import com.looksee.auditService.models.repository.AuditRepository;
import com.looksee.auditService.models.repository.DesignSystemRepository;
import com.looksee.auditService.models.repository.LabelRepository;
import com.looksee.auditService.models.repository.PageStateRepository;
import com.looksee.auditService.models.repository.UXIssueMessageRepository;

/**
 * Contains business logic for interacting with and managing audits
 *
 */
@Service
public class AuditRecordService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(AuditRecordService.class);

	@Autowired
	private AuditRecordRepository audit_record_repo;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private AuditRepository audit_repo;
	
	@Autowired
	private PageStateRepository page_state_repo;
	
	@Autowired
	private AccountRepository account_repo;
	
	@Autowired
	private DesignSystemRepository design_system_repo;
	
	@Autowired 
	private LabelRepository label_repo;
	
	@Autowired
	private UXIssueMessageRepository ux_issue_repo;
	
	public AuditRecord save(AuditRecord audit) {
		assert audit != null;

		return audit_record_repo.save(audit);
	}
	
	public AuditRecord save(AuditRecord audit, Long account_id, Long domain_id) {
		assert audit != null;

		AuditRecord audit_record = audit_record_repo.save(audit);
		
		//broadcast audit record to users
		return audit_record;
	}

	public Optional<AuditRecord> findById(long id) {
		return audit_record_repo.findById(id);
	}
	
	public AuditRecord findByKey(String key) {
		return audit_record_repo.findByKey(key);
	}


	public List<AuditRecord> findAll() {
		// TODO Auto-generated method stub
		return IterableUtils.toList(audit_record_repo.findAll());
	}
	
	public void addAudit(String audit_record_key, String audit_key) {
		//check if audit already exists for page state
		Optional<Audit> audit = audit_repo.getAuditForAuditRecord(audit_record_key, audit_key);
		if(!audit.isPresent()) {
			audit_record_repo.addAudit(audit_record_key, audit_key);
		}
	}

	public void addAudit(long audit_record_id, long audit_id) {
		assert audit_record_id != audit_id;
		
		//check if audit already exists for page state
		audit_record_repo.addAudit(audit_record_id, audit_id);
	}
	
	public Set<Audit> getAllAuditsAndIssues(long audit_id) {		
		return audit_repo.getAllAuditsForPageAuditRecord(audit_id);
	}
	
	public Optional<AuditRecord> findMostRecentDomainAuditRecord(long id) {
		return audit_record_repo.findMostRecentDomainAuditRecord(id);
	}
	
	public Optional<PageAuditRecord> findMostRecentPageAuditRecord(String page_url) {
		assert page_url != null;
		assert !page_url.isEmpty();
		
		return audit_record_repo.getMostRecentPageAuditRecord(page_url);
	}
	
	public Set<Audit> findMostRecentAuditsForPage(String page_url) {
		assert page_url != null;
		assert !page_url.isEmpty();
		
		//get most recent page state
		PageState page_state = page_state_service.findByUrl(page_url);
		return audit_repo.getMostRecentAuditsForPage(page_state.getKey());
		//return audit_record_repo.findMostRecentDomainAuditRecord(page_url);
	}

	public Set<Audit> getAllColorPaletteAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_repo.getAllPageColorPaletteAudits(audit_record_key);
	}

	public Set<Audit> getAllTextColorContrastAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_repo.getAllPageTextColorContrastAudits(audit_record_key);
	}

	public Set<Audit> getAllNonTextColorContrastAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_repo.getAllPageNonTextColorContrastAudits(audit_record_key);
	}

	public Set<Audit> getAllTypefaceAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_repo.getAllPageTypefaceAudits(audit_record_key);
	}

	
	public Set<Audit> getAllLinkAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_repo.getAllPageLinkAudits(audit_record_key);
	}

	public Set<Audit> getAllTitleAndHeaderAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_repo.getAllPageTitleAndHeaderAudits(audit_record_key);
	}

	public Set<Audit> getAllAltTextAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_repo.getAllPageAltTextAudits(audit_record_key);
	}


	public Set<Audit> getAllMarginAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_repo.getAllPageMarginAudits(audit_record_key);
	}

	public Set<Audit> getAllPagePaddingAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_repo.getAllPagePaddingAudits(audit_record_key);
	}

	public Set<Audit> getAllPageParagraphingAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_repo.getAllPageParagraphingAudits(audit_record_key);
	}

	public Set<AuditRecord> getAllPageAudits(long audit_record_id) {		
		return audit_record_repo.getAllPageAudits(audit_record_id);
	}
	
	public Set<Audit> getAllAuditsForPageAuditRecord(long page_audit_id) {		
		return audit_repo.getAllAuditsForPageAuditRecord( page_audit_id);
	}

	public void addPageAuditToDomainAudit(long domain_audit_record_id, String page_audit_record_key) {
		//check if audit already exists for page state
		audit_record_repo.addPageAuditRecord(domain_audit_record_id, page_audit_record_key);
	}


	public void addPageAuditToDomainAudit(long domain_audit_id, long page_audit_id) {
		audit_record_repo.addPageAuditRecord(domain_audit_id, page_audit_id);
	}
	
	public Optional<PageAuditRecord> getMostRecentPageAuditRecord(String url) {
		assert url != null;
		assert !url.isEmpty();
		
		return audit_record_repo.getMostRecentPageAuditRecord(url);
	}

	public Set<Audit> getAllContentAuditsForDomainRecord(long id) {
		return audit_repo.getAllContentAuditsForDomainRecord(id);
	}

	public Set<Audit> getAllInformationArchitectureAuditsForDomainRecord(long id) {
		return audit_repo.getAllInformationArchitectureAuditsForDomainRecord(id);
	}

	public Set<Audit> getAllAccessibilityAuditsForDomainRecord(long id) {
		return audit_repo.getAllAccessibilityAuditsForDomainRecord(id);
	}

	public Set<Audit> getAllAestheticAuditsForDomainRecord(long id) {
		return audit_repo.getAllAestheticsAuditsForDomainRecord(id);
	}

	public Set<Audit> getAllContentAudits(long audit_record_id) {
		return audit_repo.getAllContentAudits(audit_record_id);
	}

	public Set<Audit> getAllInformationArchitectureAudits(long id) {
		return audit_repo.getAllInformationArchitectureAudits(id);
	}

	public Set<Audit> getAllAccessibilityAudits(Long id) {
		return audit_repo.getAllAccessibilityAudits(id);
	}

	public Set<Audit> getAllAestheticAudits(long id) {
		return audit_repo.getAllAestheticsAudits(id);
	}

	public Set<UXIssueMessage> getIssues(long audit_record_id) {
		return ux_issue_repo.getIssues(audit_record_id);
	}

	public Set<PageState> getPageStatesForDomainAuditRecord(long audit_record_id) {
		return page_state_repo.getPageStatesForDomainAuditRecord(audit_record_id);
	}

	public void addPageToAuditRecord(long audit_record_id, long page_state_id) {
		audit_record_repo.addPageToAuditRecord( audit_record_id, page_state_id );
	}

	public long getIssueCountBySeverity(long id, String severity) {
		return audit_record_repo.getIssueCountBySeverity(id, severity);
	}

	public int getPageAuditCount(long domain_audit_id) {
		return audit_record_repo.getPageAuditRecordCount(domain_audit_id);
	}

	/**
	 * Retrieves all {@link Audit audits} associated with the given 
	 * 	{@link PageAuditRecord audit_record}
	 * 
	 * @param id {@link PageAuditRecord} ID value
	 * @return
	 */
	public Set<Audit> getAllAudits(long id) {
		return audit_repo.getAllAudits(id);
	}

	public boolean isDomainAuditComplete(AuditRecord audit_record) {
		//audit_record should now have a domain audit record
		//get all page audit records for domain audit

		Set<AuditRecord> page_audits = audit_record_repo.getAllPageAudits(audit_record.getId());
		if(audit_record.getDataExtractionProgress() < 1.0) {
			return false;
		}
		//check all page audit records. If all are complete then the domain is also complete
		for(AuditRecord audit : page_audits) {
			if(!audit.isComplete()) {
				return false;
			}
		}
		
		return true;
	}

	public Optional<DomainAuditRecord> getDomainAuditRecordForPageRecord(long id) {
		return audit_record_repo.getDomainForPageAuditRecord(id);
	}

	public Optional<Account> getAccount(long audit_record_id) {
		return account_repo.getAccount(audit_record_id);
	}

	public Set<Label> getLabelsForImageElements(long id) {
		return label_repo.getLabelsForImageElements(id);
	}

	public Optional<DesignSystem> getDesignSystem(long audit_record_id) {
		return design_system_repo.getDesignSystem(audit_record_id);
	}
	
	public AuditRecord addJourney(long audit_record_id, long journey_id) {
		return audit_record_repo.addJourney(audit_record_id, journey_id);
	}

	/**
	 * Update the progress for the appropriate {@linkplain AuditCategory}
	 * @param auditRecordId
	 * @param category
	 * @param account_id
	 * @param domain_id
	 * @param progress
	 * @param message
	 * @return
	 */
	public AuditRecord updateAuditProgress(long auditRecordId,
										   AuditCategory category,
										   long account_id,
										   long domain_id,
										   double progress,
										   String message)
	{
		AuditRecord audit_record = findById(auditRecordId).get();
		audit_record.setDataExtractionProgress(1.0);
		audit_record.setStatus(ExecutionStatus.RUNNING_AUDITS);

		if(AuditCategory.CONTENT.equals(category)) {
			audit_record.setContentAuditProgress( progress );
		}
		else if(AuditCategory.AESTHETICS.equals(category)) {
			audit_record.setAestheticAuditProgress( progress);
		}
		else if(AuditCategory.INFORMATION_ARCHITECTURE.equals(category)) {
			audit_record.setInfoArchitectureAuditProgress( progress );
		}
		
		return save(audit_record, account_id, domain_id);
	}

	/**
	 * Update the progress for the appropriate {@linkplain AuditCategory}
	 * @param auditRecordId
	 * @param category
	 * @param account_id
	 * @param domain_id
	 * @param progress
	 * @param message
	 * @return
	 */
	public void updateAuditProgress(long audit_record_id, 
										   double content_progress, 
										   double info_architecture_progress,
										   double aesthetic_progress,
										   double data_extraction_progress)
	{
		AuditRecord audit_record = findById(audit_record_id).get();
		audit_record.setDataExtractionProgress(data_extraction_progress);
		audit_record.setStatus(ExecutionStatus.RUNNING_AUDITS);

		audit_record.setContentAuditProgress( content_progress );
		audit_record.setAestheticAuditProgress( aesthetic_progress);
		audit_record.setInfoArchitectureAuditProgress( info_architecture_progress );
		
		audit_record_repo.updateProgress(audit_record_id, content_progress, info_architecture_progress, aesthetic_progress, data_extraction_progress);
	}

	/**
	 * Retrieves {@link PageState} with given URL for {@link DomainAuditRecord}  
	 * @param audit_record_id
	 * @param current_url
	 * @return
	 */
	public PageState findPageWithUrl(long audit_record_id, String url) {
		return page_state_repo.findPageWithUrl(audit_record_id, url);
	}
	

	@Deprecated
	public Optional<AuditRecord> getMostRecentAuditRecordForDomain(String host) {
		assert host != null;
		assert !host.isEmpty();
		
		return audit_record_repo.getMostRecentAuditRecordForDomain(host);
	}
	
	public Optional<AuditRecord> getMostRecentAuditRecordForDomain(long id) {
		return audit_record_repo.getMostRecentAuditRecordForDomain(id);
	}

	public Set<Audit> getAllAuditsForDomainAudit(long domain_audit_record_id) {
		return audit_repo.getAllAuditsForDomainAudit(domain_audit_record_id);
	}

    public boolean updateAuditScores(long audit_record_id, 
								  double content_score, 
								  double info_architecture_score,
            					  double aesthetic_score) {
		try{
       		audit_record_repo.updateScores(audit_record_id, content_score, info_architecture_score, aesthetic_score);
			return true;
		}catch(Exception e){
			return false;
		}
    }

	public int getNumberOfJourneysWithStatus(long domain_audit_id, JourneyStatus candidate) {
		return audit_record_repo.getNumberOfJourneysWithStatus(domain_audit_id, candidate.toString());
	}

	public int getNumberOfJourneys(long domain_audit_id) {
		return audit_record_repo.getNumberOfJourneys(domain_audit_id);
	}

	public PageState findPage(long audit_record_id) {
		return page_state_repo.getPageStateForAuditRecord(audit_record_id);
	}
	
	/**
	 * Retrieve {@link PageState} for the {@linkplain AuditRecord} with the given id
	 * @param page_audit_key
	 * @return
	 */
	public PageState getPageStateForAuditRecord(long audit_record_id) {
		return page_state_repo.getPageStateForAuditRecord(audit_record_id);
	}
}
