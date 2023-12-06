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
import com.looksee.auditService.models.enums.AuditCategory;
import com.looksee.auditService.models.enums.AuditLevel;
import com.looksee.auditService.models.enums.AuditName;
import com.looksee.auditService.models.enums.JourneyStatus;
import com.looksee.auditService.models.message.AuditProgressUpdate;
import com.looksee.auditService.models.message.DiscardedJourneyMessage;
import com.looksee.auditService.models.message.JourneyCandidateMessage;
import com.looksee.auditService.models.message.JourneyMappingProgressMessage;
import com.looksee.auditService.models.message.PageAuditProgressMessage;
import com.looksee.auditService.models.message.VerifiedJourneyMessage;
import com.looksee.auditService.models.repository.PageStateRepository;
import com.looksee.auditService.services.AccountService;
import com.looksee.auditService.services.AuditRecordService;
import com.looksee.auditService.services.DomainService;
import com.looksee.auditService.services.MessageBroadcaster;
import com.looksee.auditService.services.SendGridMailService;
import com.looksee.auditService.models.dto.PageAuditDto;
import com.looksee.auditService.models.enums.ExecutionStatus;
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
	private PageStateRepository page_state_repo;
	
	@Autowired
	private SendGridMailService mail_service;
	
	@Autowired
	private MessageBroadcaster pusher;
	
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body) throws JsonMappingException, JsonProcessingException, ExecutionException, InterruptedException {

		Body.Message message = body.getMessage();
		String data = message.getData();
	    String target = !data.isEmpty() ? new String(Base64.getDecoder().decode(data)) : "";


	    log.warn("message received = "+target);

	    ObjectMapper input_mapper = new ObjectMapper();
	    input_mapper.registerModule(new JavaTimeModule());
	    //JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
	    	    
	    try {
	    	AuditProgressUpdate audit_msg = input_mapper.readValue(target, AuditProgressUpdate.class);
	    	
	    	//get AuditRecord from database
	    	Optional<AuditRecord> audit_record = audit_record_service.findById(audit_msg.getAuditRecordId());
	    	if(audit_record.isPresent() && audit_record.get() instanceof PageAuditRecord) {
	    		//build page audit progress
	    		AuditUpdateDto audit_update = buildPageAuditUpdatedDto(audit_msg.getAuditRecordId());
				pusher.sendAuditUpdate(audit_record.get().getId()+"", audit_update);

	    		//if domain audit is complete then send email
	    		if( ExecutionStatus.COMPLETE.equals(audit_update.getStatus())) {
	    			log.warn("sending email to user");
	    	    	//send email that audit is complete
	    			Account account = account_service.findById(audit_msg.getAccountId()).get();
	    		    Domain domain = domain_service.findByAuditRecord(audit_msg.getAuditRecordId());
	    			mail_service.sendDomainAuditCompleteEmail(account.getEmail(), 
	    													  domain.getUrl(), 
	    													  domain.getId());
	    		}
	    		
	    		//send Pusher message
	    	}
	    	else if(audit_record.isPresent() && audit_record.get() instanceof DomainAuditRecord) {
	    		//build domain audit progress
	    		AuditUpdateDto audit_update = buildDomainAuditRecordDTO(audit_msg.getAuditRecordId());
				pusher.sendAuditUpdate(audit_record.get().getId()+"", audit_update);

	    		//if domain audit is complete then send email
	    		if( ExecutionStatus.COMPLETE.equals(audit_update.getStatus())) {
	    			log.warn("sending email to user");
	    	    	//send email that audit is complete
	    			Account account = account_service.findById(audit_msg.getAccountId()).get();
	    		    Domain domain = domain_service.findByAuditRecord(audit_msg.getAuditRecordId());
	    			mail_service.sendDomainAuditCompleteEmail(account.getEmail(), 
	    													  domain.getUrl(), 
	    													  domain.getId());
	    		}
	    		
	    		//send pusher message
	    	}
	    } catch(Exception e) {
	    
	    }

	    
	    
	    /********************************************************
	    * PAGE AUDIT PROGRESS EVENTS
	    ********************************************************/
	    //if message is audit message then update page audit
	    try {
		    PageAuditProgressMessage audit_msg = input_mapper.readValue(target, PageAuditProgressMessage.class);			
		   
		    //update audit record
		    log.warn("finding PageAudit by id = "+audit_msg.getPageAuditId());
			PageAuditRecord audit_record = (PageAuditRecord)audit_record_service.findById(audit_msg.getPageAuditId()).get();
			/*
			
			if(AuditCategory.AESTHETICS.equals(audit_msg.getCategory())) {
				audit_record.setAestheticAuditProgress(audit_msg.getProgress());
			}
			if(AuditCategory.CONTENT.equals(audit_msg.getCategory())) {
				audit_record.setContentAuditProgress(audit_msg.getProgress());
			}
			if(AuditCategory.INFORMATION_ARCHITECTURE.equals(audit_msg.getCategory())) {
				audit_record.setInfoArchitectureAuditProgress(audit_msg.getProgress());
			}
			*/
	    
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
						mail_service.sendDomainAuditCompleteEmail(account.getEmail(), domain.getUrl(), domain.getId());
					}
				}
				else {
					PageState page = page_state_repo.getPageStateForAuditRecord(audit_record.getId());							
					Account account = account_service.findById(audit_msg.getAccountId()).get();
					log.warn("sending email to account :: "+account.getEmail());
					mail_service.sendPageAuditCompleteEmail(account.getEmail(), page.getUrl(), audit_record.getId());
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
	    
	    /**
	     * When a JourneyMappingProgressMessage is encountered, broadcast an AuditUpdate message to the audit channel
	     */
	    try {
		    JourneyMappingProgressMessage audit_msg = input_mapper.readValue(target, JourneyMappingProgressMessage.class);

		    log.warn("(JourneyMappingProgress) building AuditUpdateDTO");
	    	//AuditUpdateDto audit_update = buildAuditRecordDTO(audit_msg);
		    AuditUpdateDto audit_update = buildPageAuditUpdatedDto(audit_msg.getDomainAuditRecordId());
			log.warn("sending audit record update to user");
			pusher.sendAuditUpdate(audit_msg.getDomainAuditRecordId()+"", audit_update);
			
			log.warn("sent message to user with account id = "+audit_msg.getAccountId());
			return new ResponseEntity<String>("Successfully sent audit update to user", HttpStatus.OK);
	    }
	    catch(Exception e) {
	    }

	    
	    //TODO: fixe issue with Unrecognized fields for Journey messages
	    //Map<String, JourneyStatus> status_map = new HashMap<>();
	    String journey_key = "";
	    //if input mapper can convert Journey Candidate, then 
	    //      1. Update JourneyStatus for journey key in domain audit to Candidate
	    try {
	    	log.warn("RECIEVED JourneyCandidateMessage!!!! Should this be happening?");
		    JourneyCandidateMessage journey_candidate_msg = input_mapper.readValue(target, JourneyCandidateMessage.class);
		    log.warn("retrieving journey key");
		    journey_key = journey_candidate_msg.getJourney().getKey();
		    
		    log.warn("(JourneyCandidate) finding DomainAuditRecord by id = "+journey_candidate_msg.getDomainAuditRecordId());
			AuditUpdateDto audit_update = buildDomainAuditRecordDTO(journey_candidate_msg.getDomainAuditRecordId());
		    //PageAuditDto audit_update = builPagedAuditdDto(journey_candidate_msg.getDomainAuditRecordId(), target);
			log.warn("sending audit record update to user");
			pusher.sendAuditUpdate(journey_candidate_msg.getDomainAuditRecordId()+"", audit_update);

			return new ResponseEntity<String>("Successfully sent audit update to user", HttpStatus.OK);
	    }
	    catch(Exception e) {
	    	//log.warn("error converting json string to JourneyCandidateMessage : "+e.getMessage());
	    	//e.printStackTrace();
	    }
	    
	    //if input mapper can convert Journey Verified, then 
	    //		1.  Update JourneyStatus for journey key in domain audit to Verified
	    try {
	    	VerifiedJourneyMessage verified_journey_msg = input_mapper.readValue(target, VerifiedJourneyMessage.class);

	    	log.warn("(VerifiedJourney) message deserialized");
		    journey_key = verified_journey_msg.getJourney().getKey();
		    
		    log.warn("(VerifiedJourney) finding DomainAuditRecord by id = "+verified_journey_msg.getDomainAuditRecordId());
			//AuditUpdateDto audit_update = buildAuditRecordDTO(verified_journey_msg);
		    AuditUpdateDto audit_update = buildPageAuditUpdatedDto(verified_journey_msg.getDomainAuditRecordId());
			log.warn("sending audit record update to user");
			pusher.sendAuditUpdate(verified_journey_msg.getDomainAuditRecordId()+"", audit_update);
		    
		    log.warn("journey key retrieved : " + journey_key);
			return new ResponseEntity<String>("Successfully sent audit update to user", HttpStatus.OK);
	    }
	    catch(Exception e) {
	    	//log.warn("error converting json string to VerifiedJourneyMessage : "+e.getMessage());
	    }
	    
	    //if input mapper can convert Journey Verified, then 
	    //		1. 
	    //      2. Update JourneyStatus for journey key in domain audit to Verified
	    try {
		    DiscardedJourneyMessage discarded_journey_msg = input_mapper.readValue(target, DiscardedJourneyMessage.class);
		    log.warn("DiscardedJourneyMessage identified");
		    journey_key = discarded_journey_msg.getJourney().getKey();

		    log.warn("(DiscardedJourney) finding DomainAuditRecord by id = "+discarded_journey_msg.getDomainAuditRecordId());
			//AuditUpdateDto audit_update = buildAuditRecordDTO(discarded_journey_msg);
		    AuditUpdateDto audit_update = buildPageAuditUpdatedDto(discarded_journey_msg.getDomainAuditRecordId());
			log.warn("sending audit record update to user");
			pusher.sendAuditUpdate(discarded_journey_msg.getDomainAuditRecordId()+"", audit_update);
			
			return new ResponseEntity<String>("Successfully sent audit update to user", HttpStatus.OK);

	    }
	    catch(Exception e) {
	    	//log.warn("error converting json string to DiscardedJourneyMessage : "+e.getMessage());
	    }
	    
	    
	    
	    
	    
	    // update data extraction of domain audit to equal the number of journey keys that do NOT have an empty string associated with them over the
	    //       number of journeys present in domain audit
	    
	    //if domain audit is complete, then send email to user informing them of it's completion		
		return new ResponseEntity<String>("Error occurred while updated audit progress", HttpStatus.OK);
	}

	/**
	 * Creates an {@linkplain AuditUpdateDto} using page audit ID and the provided page_url
	 * 
	 * @param pageAuditId
	 * @return
	 * 
	 * @pre level != null
	 */
	private AuditUpdateDto buildAuditUpdatedDto(long pageAuditId, AuditLevel level) {
		//get all audits
		Set<Audit> audits = audit_record_service.getAllAudits(pageAuditId);
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
																 AuditUtils.getAuditLabels(AuditCategory.AESTHETICS, audit_labels));
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

		double data_extraction_progress = 1;
		String message = "";
		ExecutionStatus execution_status = ExecutionStatus.UNKNOWN;
		if(visual_design_progress < 1 || content_progress < 1 || visual_design_progress < 1) {
			execution_status = ExecutionStatus.IN_PROGRESS;
		}
		else {
			execution_status = ExecutionStatus.COMPLETE;
		}
		
		return new AuditUpdateDto(pageAuditId,
									level,
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
	 * Creates an {@linkplain PageAuditDto} using page audit ID and the provided page_url
	 * @param pageAuditId
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
																 AuditUtils.getAuditLabels(AuditCategory.AESTHETICS, audit_labels));
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

		double data_extraction_progress = 1;
		String message = "";
		
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
	    /*
	   Set<Audit> audits = page_audits.stream().map(page -> {
		   return audit_record_service.getAllAuditsForPageAuditRecord(page.getId());
	   }).flatMap(Set<Audit>::stream).collect(Collectors.toSet());
	   */
	 //   Set<Audit> audit_list = audit_record_service.getAllAuditsForDomainAudit(domain_audit.getId());
	    //calculate percentage of audits that are currently complete for each category
		double visual_design_progress = AuditUtils.calculateProgress(AuditCategory.AESTHETICS, total_pages, audits, audit_labels);
		double content_progress = AuditUtils.calculateProgress(AuditCategory.CONTENT, total_pages, audits, audit_labels);
		double info_architecture_progress = AuditUtils.calculateProgress(AuditCategory.INFORMATION_ARCHITECTURE, total_pages, audits, audit_labels);
		
		double data_extraction_progress = getDataExtractionProgress(domain_audit);
		
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
	 * @return
	 */
	private double getDataExtractionProgress(DomainAuditRecord domain_audit) {
		assert domain_audit != null;
		
		int candidate_count = audit_record_service.getNumberOfJourneysWithStatus(domain_audit.getId(), JourneyStatus.CANDIDATE);
		int total_journeys = audit_record_service.getNumberOfJourneys(domain_audit.getId());
		
		if(total_journeys <= 1) {
			return 0.01;
		}
		
		return (double)(total_journeys - candidate_count) / (double)total_journeys;

	}

}