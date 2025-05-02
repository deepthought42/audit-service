package com.looksee.auditService;

import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.looksee.auditService.mapper.Body;
import com.looksee.auditService.models.Account;
import com.looksee.auditService.models.Audit;
import com.looksee.auditService.models.AuditRecord;
import com.looksee.auditService.models.Domain;
import com.looksee.auditService.models.DomainAuditRecord;
import com.looksee.auditService.models.PageAuditRecord;
import com.looksee.auditService.models.PageState;
import com.looksee.auditService.models.dto.AuditUpdateDto;
import com.looksee.auditService.models.dto.PageAuditDto;
import com.looksee.auditService.models.enums.AuditCategory;
import com.looksee.auditService.models.enums.AuditLevel;
import com.looksee.auditService.models.enums.AuditName;
import com.looksee.auditService.models.enums.ExecutionStatus;
import com.looksee.auditService.models.enums.JourneyStatus;
import com.looksee.auditService.models.message.AuditProgressUpdate;
import com.looksee.auditService.models.message.DiscardedJourneyMessage;
import com.looksee.auditService.models.message.JourneyCandidateMessage;
import com.looksee.auditService.models.message.PageAuditProgressMessage;
import com.looksee.auditService.models.message.VerifiedJourneyMessage;
import com.looksee.auditService.services.AccountService;
import com.looksee.auditService.services.AuditRecordService;
import com.looksee.auditService.services.DomainService;
import com.looksee.auditService.services.MessageBroadcaster;
import com.looksee.auditService.services.PageStateService;
import com.looksee.utils.AuditUtils;

@RestController
public class AuditController {
	private static Logger log = LoggerFactory.getLogger(AuditController.class);

	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private AccountService account_service;
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private MessageBroadcaster pusher;
	
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body) throws JsonMappingException, JsonProcessingException, ExecutionException, InterruptedException {

		Body.Message message = body.getMessage();
		String data = message.getData();
		String target = !data.isEmpty() ? new String(Base64.getDecoder().decode(data)) : "";


		log.warn("message received = "+target);

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());

		try {
			AuditProgressUpdate audit_msg = mapper.readValue(target, AuditProgressUpdate.class);
			log.warn("audit progress update message detected");
	    	//get AuditRecord from database
			Optional<AuditRecord> audit_record = audit_record_service.findById(audit_msg.getPageAuditId());
	
			if(audit_record.isPresent()) {
				log.warn("PageAuditRecord found");
	    		//build page audit progress
				AuditUpdateDto audit_update = buildPageAuditUpdatedDto(audit_msg.getPageAuditId());
				pusher.sendAuditUpdate(audit_record.get().getId()+"", audit_update);

				Optional<DomainAuditRecord> domain_audit_record_opt = audit_record_service.getDomainAuditRecordForPageRecord(audit_msg.getPageAuditId());
				if(domain_audit_record_opt.isPresent()){
					
					audit_update = buildDomainAuditRecordDTO(audit_msg.getPageAuditId());
					pusher.sendAuditUpdate(audit_record.get().getId()+"", audit_update);

					if( ExecutionStatus.COMPLETE.equals(audit_update.getStatus())) {
						Account account = account_service.findById(audit_msg.getAccountId()).get();
						if(account != null){
							Domain domain = domain_service.findByAuditRecord(domain_audit_record_opt.get().getId());

							log.warn("sending email to user = "+account.getEmail());
							//mail_service.sendDomainAuditCompleteEmail(account.getEmail(),
							//								domain.getUrl(),
							//								domain.getId());
						}
					}
				}
				else{
					//if domain audit is complete then send email
					if( ExecutionStatus.COMPLETE.equals(audit_update.getStatus())) {
						log.warn("sending email to user");
						//send email that audit is complete
						Account account = account_service.findById(audit_msg.getAccountId()).get();
						//Account account = account_service.findForAuditRecord(audit_msg.getPageAuditId());
						PageState page_state = audit_record_service.getPageStateForAuditRecord(audit_msg.getPageAuditId());
						
						if(account != null){
							log.warn("sending email to user = "+account.getEmail());
							//mail_service.sendPageAuditCompleteEmail(account.getEmail(), 
																	  //page_state.getUrl(), 
																	  //page_state.getId());
						}
					}
				}
			}
			else {
				log.warn("UKNOWN record type found");
			}

			return new ResponseEntity<String>("Successfully sent audit update to user", HttpStatus.OK);
		} catch(Exception e) {
			e.printStackTrace();
		}

		/********************************************************
	    * PAGE AUDIT PROGRESS EVENTS
	    ********************************************************/
	    //if message is audit message then update page audit
		try {
			PageAuditProgressMessage audit_msg = mapper.readValue(target, PageAuditProgressMessage.class);
		    //update audit record
			log.warn("finding PageAudit by id = "+audit_msg.getPageAuditId());
			PageAuditRecord audit_record = (PageAuditRecord)audit_record_service.findById(audit_msg.getPageAuditId()).get();

			log.warn("retrieving all audits");
			Set<Audit> audit_list = audit_record_service.getAllAudits(audit_msg.getPageAuditId());
			log.warn("collecting audit labels");
			Set<AuditName> audit_labels = audit_record.getAuditLabels();
			
			//if page audit is complete then 
			boolean is_page_audit_complete = AuditUtils.isPageAuditComplete(audit_list, audit_labels);
			
			Optional<DomainAuditRecord> domain_audit = audit_record_service.getDomainAuditRecordForPageRecord(audit_record.getId());

			if(is_page_audit_complete) {
				long audit_id = audit_record.getId();
				
				//if domainASudit is present and considered complete then send an email to the user if the account exists
				if(domain_audit.isPresent()) {
					boolean is_domain_audit_complete = audit_record_service.isDomainAuditComplete(domain_audit.get());
					audit_id = domain_audit.get().getId();
					
					if(is_domain_audit_complete) {
						Domain domain = domain_service.findByAuditRecord(audit_id);
						Account account = account_service.findById(audit_msg.getAccountId()).get();
						log.warn("sending email to account :: "+audit_id);
						//mail_service.sendDomainAuditCompleteEmail(account.getEmail(), domain.getUrl(), domain.getId());
					}
				}
				else {
					PageState page = audit_record_service.getPageStateForAuditRecord(audit_record.getId());
					Account account = account_service.findById(audit_msg.getAccountId()).get();
					log.warn("sending email to account :: "+account.getEmail());
					//mail_service.sendPageAuditCompleteEmail(account.getEmail(), page.getUrl(), audit_record.getId());
				}
			}
			
			// If domain audit exists send a domain level audit update
			if(domain_audit.isPresent()) {
				 //Broadcast audit update message to pusher
				AuditUpdateDto audit_update = buildPageAuditUpdatedDto(domain_audit.get().getId());
				log.warn("sending audit record update to user");
				pusher.sendAuditUpdate(domain_audit.get().getId()+"", audit_update);
			}
			else {
				 //Broadcast audit update message to pusher
				AuditUpdateDto audit_update = buildPageAuditUpdatedDto(audit_record.getId());
				log.warn("sending audit record update to user");
				pusher.sendAuditUpdate(audit_record.getId()+"", audit_update);
			}
			
			log.warn("successfully sent update for single page audit");
			return new ResponseEntity<String>("Successfully sent audit update to user", HttpStatus.OK);
	    }
	    catch(Exception e) {
	    	//log.warn("An exception occurred while converting JSON to AuditProgressUpdate : "+e.getMessage());
	    	//e.printStackTrace();
	    }

	    try {
		    JourneyCandidateMessage journey_candidate_msg = mapper.readValue(target, JourneyCandidateMessage.class);
		    log.warn("Received JourneyCandidateMessage!!!! Should this be happening?");
		    
			AuditUpdateDto audit_update = buildDomainAuditRecordDTO(journey_candidate_msg.getAuditRecordId());
			pusher.sendAuditUpdate(journey_candidate_msg.getAuditRecordId()+"", audit_update);

			return new ResponseEntity<String>("Successfully sent audit update to user", HttpStatus.OK);
	    }
	    catch(Exception e) {
	    	//log.warn("error converting json string to JourneyCandidateMessage : "+e.getMessage());
	    	//e.printStackTrace();
	    }
	    
	    try {
	    	VerifiedJourneyMessage verified_journey_msg = mapper.readValue(target, VerifiedJourneyMessage.class);

	    	log.warn("(VerifiedJourney) message deserialized");
		    
			AuditUpdateDto audit_update = buildDomainAuditRecordDTO(verified_journey_msg.getDomainAuditRecordId());
			pusher.sendAuditUpdate(verified_journey_msg.getDomainAuditRecordId()+"", audit_update);
			return new ResponseEntity<String>("Successfully sent audit update to user", HttpStatus.OK);
	    }
	    catch(Exception e) {
	    	//log.warn("error converting json string to VerifiedJourneyMessage : "+e.getMessage());
	    }
	    
	    try {
		    DiscardedJourneyMessage discarded_journey_msg = mapper.readValue(target, DiscardedJourneyMessage.class);
		    log.warn("DiscardedJourneyMessage message deserialized");

		    AuditUpdateDto audit_update = buildDomainAuditRecordDTO(discarded_journey_msg.getDomainAuditRecordId());
			pusher.sendAuditUpdate(discarded_journey_msg.getDomainAuditRecordId()+"", audit_update);
			
			return new ResponseEntity<String>("Successfully sent audit update to user", HttpStatus.OK);

	    }
	    catch(Exception e) {
	    	//log.warn("error converting json string to DiscardedJourneyMessage : "+e.getMessage());
	    }
	    
		return new ResponseEntity<String>("Error occurred while updated audit progress", HttpStatus.OK);
	}
	
	/**
	 * Creates an {@linkplain PageAuditDto} using page audit ID and the provided page_url
	 * 
	 * @param pageAuditId
	 * 
	 * @return
	 */
	private AuditUpdateDto buildPageAuditUpdatedDto(long page_audit_id) {
		//get all audits
		Set<Audit> audits = audit_record_service.getAllAudits(page_audit_id);
		Set<AuditName> audit_labels = new HashSet<AuditName>();
		audit_labels.add(AuditName.TEXT_BACKGROUND_CONTRAST);
		audit_labels.add(AuditName.NON_TEXT_BACKGROUND_CONTRAST);
		audit_labels.add(AuditName.TITLES);
		audit_labels.add(AuditName.IMAGE_COPYRIGHT);
		audit_labels.add(AuditName.IMAGE_POLICY);
		audit_labels.add(AuditName.LINKS);
		audit_labels.add(AuditName.ALT_TEXT);
		audit_labels.add(AuditName.METADATA);
		audit_labels.add(AuditName.READING_COMPLEXITY);
		audit_labels.add(AuditName.PARAGRAPHING);
		audit_labels.add(AuditName.ENCRYPTED);
		//count audits for each category
		//calculate content score
		//calculate aesthetics score
		//calculate information architecture score
		double visual_design_progress = AuditUtils.calculateProgress(AuditCategory.AESTHETICS, 
																 1, 
																 audits, 
																 AuditUtils.getAuditLabels(AuditCategory.AESTHETICS, 
																 audit_labels));
		
		double content_progress = AuditUtils.calculateProgress(AuditCategory.CONTENT, 
																1, 
																audits, 
																audit_labels);
		
		double info_architecture_progress = AuditUtils.calculateProgress(AuditCategory.INFORMATION_ARCHITECTURE, 
																		1, 
																		audits, 
																		audit_labels);

		double content_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.CONTENT);
		double info_architecture_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.INFORMATION_ARCHITECTURE);
		double visual_design_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.AESTHETICS);
		double a11y_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.ACCESSIBILITY);

		double data_extraction_progress = getPageDataExtractionProgress(page_audit_id);
		String message = "";
		if(data_extraction_progress < 0.5) {
			message = "Setting up browser";
		}
		else if(data_extraction_progress < 0.6) {
			message = "Analyzing elements";
		}
		
		ExecutionStatus execution_status = ExecutionStatus.UNKNOWN;
		if(visual_design_progress < 1 || content_progress < 1 || visual_design_progress < 1) {
			execution_status = ExecutionStatus.IN_PROGRESS;
		}
		else {
			execution_status = ExecutionStatus.COMPLETE;
		}
		
		return new AuditUpdateDto( page_audit_id,
									AuditLevel.PAGE,
									content_score,
									content_progress,
									info_architecture_score,
									info_architecture_progress,
									a11y_score,
									visual_design_score,
									visual_design_progress,
									data_extraction_progress,
									message, 
									execution_status);
	}
	
	/**
	 * Build audit {@link AuditRecordDTO progress update} for a {@link DomainAuditRecord domain audit}
	 * 
	 * @param audit_msg
	 * 
	 * @return
	 */
	private AuditUpdateDto buildDomainAuditRecordDTO(long audit_record_id) {
		DomainAuditRecord domain_audit = (DomainAuditRecord)audit_record_service.findById(audit_record_id).get();
	    Set<AuditRecord> page_audits = audit_record_service.getAllPageAudits(domain_audit.getId());
	    log.warn("total page audits found = "+page_audits.size());
	    int total_pages = page_audits.size();
	    Set<AuditName> audit_labels = domain_audit.getAuditLabels();
	   
	    Set<Audit> audits = new HashSet<Audit>();
	    for(AuditRecord page_audit: page_audits) {
	    	audits.addAll(audit_record_service.getAllAuditsForPageAuditRecord(page_audit.getId()));
	    }
	  
	    //calculate percentage of audits that are currently complete for each category
		double visual_design_progress = AuditUtils.calculateProgress(AuditCategory.AESTHETICS, total_pages, audits, audit_labels);
		double content_progress = AuditUtils.calculateProgress(AuditCategory.CONTENT, total_pages, audits, audit_labels);
		double info_architecture_progress = AuditUtils.calculateProgress(AuditCategory.INFORMATION_ARCHITECTURE, total_pages, audits, audit_labels);
		
		double data_extraction_progress = getDomainDataExtractionProgress(domain_audit);
		
		double content_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.CONTENT);
		double info_architecture_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.INFORMATION_ARCHITECTURE);
		double visual_design_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.AESTHETICS);
		double a11y_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.ACCESSIBILITY);
			
		ExecutionStatus execution_status = ExecutionStatus.UNKNOWN;
		if(visual_design_progress < 1 || content_progress < 1 || visual_design_progress < 1 || data_extraction_progress < 1) {
			execution_status = ExecutionStatus.IN_PROGRESS;
		}
		else {
			execution_status = ExecutionStatus.COMPLETE;
		}
		
		String message = "";
				
		return new AuditUpdateDto( audit_record_id,
									AuditLevel.DOMAIN,
									content_score,
									content_progress,
									info_architecture_score,
									info_architecture_progress,
									a11y_score,
									visual_design_score,
									visual_design_progress,
									data_extraction_progress,
									message, 
									execution_status);
	}
	
	/**
	 * Retrieves journeys from the domain audit and calculates a value between 0 and 1 that indicates the progress
	 * based on the number of journey's that are still in the CANDIDATE status vs the journeys that don't have the CANDIDATE STATUS
	 * 
	 * @param domain_audit
	 * 
	 * @return
	 */
	private double getDomainDataExtractionProgress(DomainAuditRecord domain_audit) {
		assert domain_audit != null;
		
		int candidate_count = audit_record_service.getNumberOfJourneysWithStatus(domain_audit.getId(), JourneyStatus.CANDIDATE);
		int total_journeys = audit_record_service.getNumberOfJourneys(domain_audit.getId());
		
		if(total_journeys <= 1) {
			return 0.01;
		}
		
		return (double)(total_journeys - candidate_count) / (double)total_journeys;

	}
	
	/**
	 * Retrieves journeys from the domain audit and calculates a value between 0 and 1 that indicates the progress
	 * based on the number of journey's that are still in the CANDIDATE status vs the journeys that don't have the CANDIDATE STATUS
	 * 
	 * NOTE : Progress is based on a magic number(10000). Be aware that all progress will be based on an assumed maximum element 
	 *        count of 1000
	 * 
	 * @param audit_record_id
	 * 
	 * @return progress percentage as a value between 0 and 1
	 */
	private double getPageDataExtractionProgress(long audit_record_id) {
		double milestone_count = 0;
		
		PageState page = audit_record_service.findPage(audit_record_id);
		
		int audit_count = audit_record_service.getAllAudits(audit_record_id).size();
		//if the audit_record has audits return 1
		if(audit_count > 0) {
			return 1.0;
		}
		
		//if audit_record has page associated with it add 1 point
		if(page != null) {
			milestone_count += 1;
		}
		else {
			return 0.0;
		}
		
		int element_count = page_state_service.getElementStateCount(page.getId());
		
		//if the associated page has elements add 1000/element_count
		int max_elements = 1000;
		if(element_count > 0) {
			if(element_count > max_elements) {
				max_elements = element_count;
			}
			milestone_count += max_elements / (double)element_count;
		}
		
		return milestone_count / 2.0;
	}
}