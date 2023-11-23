package com.looksee.auditService;

import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
import com.looksee.auditService.models.message.DiscardedJourneyMessage;
import com.looksee.auditService.models.message.DomainAuditMessage;
import com.looksee.auditService.models.message.JourneyCandidateMessage;
import com.looksee.auditService.models.message.JourneyMappingProgressMessage;
import com.looksee.auditService.models.message.PageAuditProgressMessage;
import com.looksee.auditService.models.message.VerifiedJourneyMessage;
import com.looksee.auditService.models.repository.PageStateRepository;
import com.looksee.auditService.services.AccountService;
import com.looksee.auditService.services.AuditRecordService;
import com.looksee.auditService.services.DomainService;
import com.looksee.auditService.services.SendGridMailService;
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
	
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body) throws JsonMappingException, JsonProcessingException, ExecutionException, InterruptedException {

		Body.Message message = body.getMessage();
		String data = message.getData();
	    String target = !data.isEmpty() ? new String(Base64.getDecoder().decode(data)) : "";


	    log.warn("message received = "+target);

	    ObjectMapper input_mapper = new ObjectMapper();
	    input_mapper.registerModule(new JavaTimeModule());
	    //JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
	    
	    boolean is_known_type = false;
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
			
			if(is_page_audit_complete) {
				PageState page = page_state_repo.getPageStateForAuditRecord(audit_record.getId());								
				Account account = account_service.findById(audit_msg.getAccountId()).get();
				log.warn("sending email to account :: "+account.getEmail());
				mail_service.sendPageAuditCompleteEmail(account.getEmail(), page.getUrl(), audit_record.getId());
			}
			log.warn("successfully sent update for single page audit");
			return new ResponseEntity<String>("Successfully sent audit update to user", HttpStatus.OK);
	    }
	    catch(Exception e) {
	    	//log.warn("An exception occurred while converting JSON to AuditProgressUpdate : "+e.getMessage());
	    	//e.printStackTrace();
	    }
	    
	    /********************************************************
	    * DOMAIN AUDIT PROGRESS EVENTS
	    ********************************************************/
	    try {
		    DomainAuditMessage domain_audit_msg = input_mapper.readValue(target, DomainAuditMessage.class);

		    log.warn("(DomainAuditMessage) finding DomainAuditRecord by id = "+domain_audit_msg.getDomainAuditRecordId());
		    DomainAuditRecord domain_audit = (DomainAuditRecord)audit_record_service.findById(domain_audit_msg.getDomainAuditRecordId()).get();
		    
		    boolean is_domain_audit_complete = true; //AuditUtils.isDomainAuditComplete(audit_list, audit_labels);
			
		    /*
			if(is_domain_audit_complete) {
				Domain domain = domain_service.findByAuditRecord(domain_audit.getId());								
				Account account = account_service.findById(domain_audit_msg.getAccountId()).get();
				log.warn("sending email to account :: "+account.getEmail());
				mail_service.sendDomainAuditCompleteEmail(account.getEmail(), domain.getUrl(), domain.getId());
			}
			else {
				//TODO  
				// RETRIEVE DOMAIN AUDIT STATS
		    	// SEND DOMAIN AUDIT STATS TO USER VIA PUSHER
			}
		    */
		    
		    log.warn("Domain Audit Message deserialized");
			return new ResponseEntity<String>("Successfully sent audit update to user", HttpStatus.OK);

	    }
	    catch(Exception e) {
	    	//log.warn("error converting json string to DiscardedJourneyMessage : "+e.getMessage());
	    }
	    
	    try {
		    JourneyMappingProgressMessage audit_msg = input_mapper.readValue(target, JourneyMappingProgressMessage.class);

		    log.warn("(JourneyMappingProgress) building AuditUpdateDTO");
	    	AuditUpdateDto audit_update = buildAuditRecordDTO(audit_msg);
			log.warn("sending audit record update to user");
			MessageBroadcaster.sendAuditUpdate(audit_msg.getAccountId()+"", audit_update);
			
			log.warn("sent message to user with account id = "+audit_msg.getAccountId());
			return new ResponseEntity<String>("Successfully sent audit update to user", HttpStatus.OK);
	    }
	    catch(Exception e) {
	    	//log.warn("An exception occurred while converting JSON to DomainMappingProgressMessage : "+e.getMessage());
	    }

	    
	    //TODO: fixe issue with Unrecognized fields for Journey messages
	    Map<String, JourneyStatus> status_map = new HashMap<>();
	    String journey_key = "";
	    //if input mapper can convert Journey Candidate, then 
	    //      1. Update JourneyStatus for journey key in domain audit to Candidate
	    try {
		    JourneyCandidateMessage journey_candidate_msg = input_mapper.readValue(target, JourneyCandidateMessage.class);
		    log.warn("retrieving journey key");
		    journey_key = journey_candidate_msg.getJourney().getKey();
		    
		    log.warn("(JourneyCandidate) finding DomainAuditRecord by id = "+journey_candidate_msg.getDomainAuditRecordId());
			AuditUpdateDto audit_update = buildAuditRecordDTO(journey_candidate_msg);
			log.warn("sending audit record update to user");
			MessageBroadcaster.sendAuditUpdate(journey_candidate_msg.getAccountId()+"", audit_update);

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
			AuditUpdateDto audit_update = buildAuditRecordDTO(verified_journey_msg);
			log.warn("sending audit record update to user");
			MessageBroadcaster.sendAuditUpdate(verified_journey_msg.getAccountId()+"", audit_update);
		    
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
			AuditUpdateDto audit_update = buildAuditRecordDTO(discarded_journey_msg);
			log.warn("sending audit record update to user");
			MessageBroadcaster.sendAuditUpdate(discarded_journey_msg.getAccountId()+"", audit_update);
			
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
	 * Build audit {@link AuditRecordDTO progress update} for a domain audit
	 * 
	 * @param audit_msg
	 * @return
	 */
	private AuditUpdateDto buildAuditRecordDTO(JourneyMappingProgressMessage audit_msg) {
		DomainAuditRecord domain_audit = (DomainAuditRecord)audit_record_service.findById(audit_msg.getDomainAuditRecordId()).get();
	    Set<AuditRecord> page_audits = audit_record_service.getAllPageAudits(audit_msg.getDomainAuditRecordId());
	    log.warn("total page audits found = "+page_audits.size());
	    int total_pages = page_audits.size();
	    Set<AuditName> audit_labels = domain_audit.getAuditLabels();
	    Set<Audit> audit_list = audit_record_service.getAllAuditsForDomainAudit(domain_audit.getId());
	    //calculate percentage of audits that are currently complete for each category
		double aesthetic_progress = AuditUtils.calculateProgress(AuditCategory.AESTHETICS, total_pages, audit_list, audit_labels);
		double content_progress = AuditUtils.calculateProgress(AuditCategory.CONTENT, total_pages, audit_list, audit_labels);;
		double info_architecture_progress = AuditUtils.calculateProgress(AuditCategory.INFORMATION_ARCHITECTURE, total_pages, audit_list, audit_labels);;
		

		double data_extraction_progress = domain_audit.getDataExtractionProgress();
		log.warn("data extraction progress = "+data_extraction_progress);
		
		//retrieve all journeys for domain audit
		double overall_progress = (data_extraction_progress
									+ aesthetic_progress
									+ content_progress
									+ info_architecture_progress)/4;
		
		log.warn("Total audits that are still in progress = "+page_audits.size());
		log.warn("Overall Progress = "+overall_progress);
		//if domain audit is complete then send email
		if( overall_progress >= 1 ) {
			log.warn("sending email to user");
	    	//send email that audit is complete
			Account account = account_service.findById(audit_msg.getAccountId()).get();
		    Domain domain = domain_service.findByAuditRecord(audit_msg.getDomainAuditRecordId());
			mail_service.sendDomainAuditCompleteEmail(account.getEmail(), 
													  domain.getUrl(), 
													  domain.getId());
		}
		
		int complete_pages = (int)Math.floor(((aesthetic_progress
											+ content_progress
											+ info_architecture_progress)/3)*total_pages);
		
		//build auditUpdate DTO . 
		/*
		 * This message consists of the following:
		 * 
		 *     - Audit Record ID - integer
		 *     - Audit record type - [Page, Domain]
		 *     - Data Extraction audit progress - decimal 0-1 inclusive
		 *     - Aesthetic Audit progress - decimal 0-1 inclusive
		 *     - Content Audit progress - decimal 0-1 inclusive
		 *     - Information Architecture audit progress - decimal 0-1 inclusive
		 *     - overall progress - decimal 0-1 inclusive
		 */
		
		//set values needed for auditUpdateDto
		int audit_record_id = 0;
		AuditLevel audit_type = AuditLevel.DOMAIN;
		
		return new AuditUpdateDto( audit_record_id,
								   audit_type,
								   data_extraction_progress,
								   aesthetic_progress,
								   content_progress,
								   info_architecture_progress,
								   overall_progress,
								   complete_pages, 
								   total_pages);
	}
	
	
	/**
	 * Build audit {@link AuditRecordDTO progress update} for a {@link DomainAuditRecord domain audit}
	 * 
	 * @param audit_msg
	 * @return
	 */
	private AuditUpdateDto buildAuditRecordDTO(DomainAuditMessage domain_audit_msg) {
		DomainAuditRecord domain_audit = (DomainAuditRecord)audit_record_service.findById(domain_audit_msg.getDomainAuditRecordId()).get();
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
		double aesthetic_progress = AuditUtils.calculateProgress(AuditCategory.AESTHETICS, total_pages, audits, audit_labels);
		double content_progress = AuditUtils.calculateProgress(AuditCategory.CONTENT, total_pages, audits, audit_labels);
		double info_architecture_progress = AuditUtils.calculateProgress(AuditCategory.INFORMATION_ARCHITECTURE, total_pages, audits, audit_labels);
		
		double data_extraction_progress = getDataExtractionProgress(domain_audit);
		
		log.warn("data extraction progress = "+data_extraction_progress);
		
		//retrieve all journeys for domain audit
		double overall_progress = (data_extraction_progress
									+ aesthetic_progress
									+ content_progress
									+ info_architecture_progress)/4;
		
		log.warn("Total audits that are still in progress = "+page_audits.size());
		//if domain audit is complete then send email
		if( overall_progress >= 1 ) {
			log.warn("sending email to user");
	    	//send email that audit is complete
			Account account = account_service.findById(domain_audit_msg.getAccountId()).get();
		    Domain domain = domain_service.findByAuditRecord(domain_audit_msg.getDomainAuditRecordId());
			mail_service.sendDomainAuditCompleteEmail(account.getEmail(), 
													  domain.getUrl(), 
													  domain.getId());
		}
		
		int complete_pages = (int)Math.floor(((aesthetic_progress
											+ content_progress
											+ info_architecture_progress)/3)*total_pages);
		
		//build auditUpdate DTO . 
		/*
		 * This message consists of the following:
		 * 
		 *     - Audit Record ID - integer
		 *     - Audit record type - [Page, Domain]
		 *     - Data Extraction audit progress - decimal 0-1 inclusive
		 *     - Aesthetic Audit progress - decimal 0-1 inclusive
		 *     - Content Audit progress - decimal 0-1 inclusive
		 *     - Information Architecture audit progress - decimal 0-1 inclusive
		 *     - overall progress - decimal 0-1 inclusive
		 */
		
		//set values needed for auditUpdateDto
		int audit_record_id = 0;
		AuditLevel audit_type = AuditLevel.DOMAIN;
		
		return new AuditUpdateDto( audit_record_id,
								   audit_type,
								   data_extraction_progress,
								   aesthetic_progress,
								   content_progress,
								   info_architecture_progress,
								   overall_progress,
								   complete_pages, 
								   total_pages);
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

	/**
	 * Build an AuditRecordDTO for a single page audit progress update
	 * 
	 * @param audit_msg
	 * @return
	 */
	private AuditUpdateDto buildAuditRecordDTO(PageAuditProgressMessage audit_msg) {
		PageAuditRecord page_audit = (PageAuditRecord)audit_record_service.findById(audit_msg.getPageAuditId()).get();
	    Set<AuditName> audit_labels = page_audit.getAuditLabels();
	    Set<Audit> audit_list = audit_record_service.getAllAuditsForDomainAudit(page_audit.getId());
	    
	    //calculate percentage of audits that are currently complete for each category
		double aesthetic_progress = AuditUtils.calculateProgress(AuditCategory.AESTHETICS, 1, audit_list, audit_labels);
		double content_progress = AuditUtils.calculateProgress(AuditCategory.CONTENT, 1, audit_list, audit_labels);;
		double info_architecture_progress = AuditUtils.calculateProgress(AuditCategory.INFORMATION_ARCHITECTURE, 1, audit_list, audit_labels);;
		

		double data_extraction_progress = page_audit.getDataExtractionProgress();
		log.warn("data extraction progress = "+data_extraction_progress);
		
		//retrieve all journeys for domain audit
		double overall_progress = data_extraction_progress
									+ aesthetic_progress
									+ content_progress
									+ info_architecture_progress;
		
		//if domain audit is complete then send email
		if( overall_progress >= 1 ) {
			log.warn("sending email to user");
	    	//send email that audit is complete
			Account account = account_service.findById(audit_msg.getAccountId()).get();
			//Domain domain = domain_service.findById(audit_msg.getDomainId()).get();
			mail_service.sendPageAuditCompleteEmail(account.getEmail(), 
													  page_audit.getUrl(), 
													  audit_msg.getPageAuditId());
		}
		
		int complete_audits = (int)Math.floor(((aesthetic_progress
											+ content_progress
											+ info_architecture_progress)/3));
		
		//build auditUpdate DTO . 
		/*
		 * This message consists of the following:
		 * 
		 *     - Audit Record ID - integer
		 *     - Audit record type - [Page, Domain]
		 *     - Data Extraction audit progress - decimal 0-1 inclusive
		 *     - Aesthetic Audit progress - decimal 0-1 inclusive
		 *     - Content Audit progress - decimal 0-1 inclusive
		 *     - Information Architecture audit progress - decimal 0-1 inclusive
		 *     - overall progress - decimal 0-1 inclusive
		 */
		
		//set values needed for auditUpdateDto
		int audit_record_id = 0;
		AuditLevel audit_type = AuditLevel.PAGE;	
		
		return new AuditUpdateDto( audit_record_id,
								   audit_type,
								   data_extraction_progress,
								   aesthetic_progress,
								   content_progress,
								   info_architecture_progress,
								   overall_progress,
								   complete_audits, 
								   1);
	}
  /*
  public void publishMessage(String messageId, Map<String, String> attributeMap, String message) throws ExecutionException, InterruptedException {
      log.info("Sending Message to the topic:::");
      PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
              .putAllAttributes(attributeMap)
              .setData(ByteString.copyFromUtf8(message))
              .setMessageId(messageId)
              .build();

      pubSubPublisherImpl.publish(pubsubMessage);
  }
  */
}
// [END run_pubsub_handler]
// [END cloudrun_pubsub_handler]