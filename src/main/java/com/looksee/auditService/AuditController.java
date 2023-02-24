package com.looksee.auditService;

/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// [START cloudrun_pubsub_handler]
// [START run_pubsub_handler]
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
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
import com.looksee.auditService.models.message.VerifiedJourneyMessage;
import com.looksee.auditService.services.AccountService;
import com.looksee.auditService.services.AuditRecordService;
import com.looksee.auditService.services.DomainService;
import com.looksee.auditService.services.SendGridMailService;
import com.looksee.utils.AuditUtils;

// PubsubController consumes a Pub/Sub message.
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
	private SendGridMailService mail_service;
	
	
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body) throws JsonMappingException, JsonProcessingException, ExecutionException, InterruptedException {

		Body.Message message = body.getMessage();
		String data = message.getData();
	    String target = !data.isEmpty() ? new String(Base64.getDecoder().decode(data)) : "";
        log.warn("page audit msg received = "+target);

	    ObjectMapper input_mapper = new ObjectMapper();
	    input_mapper.registerModule(new JavaTimeModule());
	    //JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
	    
	    //if message is audit message then update page audit
	    try {
		    AuditProgressUpdate audit_msg = input_mapper.readValue(target, AuditProgressUpdate.class);
			//update audit record
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
		    
		    Set<Audit> audit_list = audit_record_service.getAllAudits(audit_msg.getPageAuditId());
		    List<AuditName> audit_labels = audit_record.getAuditLabels();
			//if page audit is complete then 
			boolean is_page_audit_complete = AuditUtils.isPageAuditComplete(audit_list, audit_labels);
			
			if(is_page_audit_complete && audit_msg.getDomainAuditRecordId() < 0) {
				//audit_record.setEndTime(LocalDateTime.now());
				//audit_record.setStatus(ExecutionStatus.COMPLETE);
				
				//if it's a page audit then send page audit complete email
				//if(audit_msg.getDomainAuditRecordId() < 0) {
					PageState page = audit_record_service.getPageStateForAuditRecord(audit_record.getId());								
					Account account = account_service.findById(audit_msg.getAccountId()).get();
					log.warn("sending email to account :: "+account.getEmail());
					mail_service.sendPageAuditCompleteEmail(account.getEmail(), page.getUrl(), audit_record.getId());
				//}
			}
			
			//audit_record = (PageAuditRecord)audit_record_service.save(audit_record);	
			
			if(audit_msg.getDomainAuditRecordId() >= 0) {
				
				DomainAuditRecord domain_audit = (DomainAuditRecord)audit_record_service.findById(audit_msg.getDomainAuditRecordId()).get();
			    Set<PageAuditRecord> page_audits = audit_record_service.getAllPageAudits(audit_msg.getDomainAuditRecordId());
			    log.warn("total page audits found = "+page_audits.size());
			    int total_pages = page_audits.size();
			    
			    //calculate percentage of audits that are currently complete for each category
				double aesthetic_progress = calculateProgress(AuditCategory.AESTHETICS, total_pages, audit_list, audit_labels);
				double content_progress = calculateProgress(AuditCategory.CONTENT, total_pages, audit_list, audit_labels);;
				double info_architecture_progress = calculateProgress(AuditCategory.INFORMATION_ARCHITECTURE, total_pages, audit_list, audit_labels);;
				
				
				//if domain audit is complete then send email
				page_audits = page_audits.stream()
											.filter(audit -> audit.getAestheticAuditProgress() < 1.0)
											.filter(audit -> audit.getContentAuditProgress() < 1.0)
											.filter(audit -> audit.getInfoArchitechtureAuditProgress() < 1.0)
											.collect(Collectors.toSet());
	
				log.warn("Total audits that are still in progress = "+page_audits.size());
				//if page audits is empty, then all audits are complete
				if(page_audits.isEmpty() ) {
					log.warn("sending email to user");
				    if( domain_audit.getDataExtractionProgress() == 1 ){
				    	//send email that audit is complete
						Account account = account_service.findById(audit_msg.getAccountId()).get();
						Domain domain = domain_service.findById(audit_msg.getDomainId()).get();
						mail_service.sendDomainAuditCompleteEmail(account.getEmail(), 
																  domain.getUrl(), 
																  audit_msg.getDomainId());
				    }
				}
				
				int complete_pages = page_audits.size();
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
				AuditLevel audit_type = AuditLevel.UNKNOWN;
				
				if(audit_msg.getDomainAuditRecordId() >= 0) {
					audit_type = AuditLevel.DOMAIN;
				}
				else {
					audit_type = AuditLevel.PAGE;
				}				
				
				double data_extraction_progress = domain_audit.getDataExtractionProgress();
			
				double overall_progress = data_extraction_progress
											+ aesthetic_progress
											+ content_progress
											+ info_architecture_progress;
				
				AuditUpdateDto audit_update = new AuditUpdateDto( audit_record_id,
																  audit_type,
																  data_extraction_progress,
																  aesthetic_progress,
																  content_progress,
																  info_architecture_progress,
																  overall_progress,
																  complete_pages, 
																  total_pages);
				//send auditUpdateDTO to user
				log.warn("sending audit record update to user");
				MessageBroadcaster.sendAuditUpdate(audit_msg.getAccountId()+"", audit_update);
				
				log.warn("sent message to user with account id = "+audit_msg.getAccountId());
			}
			
			return new ResponseEntity<String>("Successfully saved updated audit record", HttpStatus.OK);			
	    }
	    catch(Exception e) {
	    	log.warn("An exception occurred while converting JSON to AuditProgressUpdate : "+e.getMessage());
	    	//e.printStackTrace();
	    }
	    
	    //Get Domain Audit Record
	  
	    
	    //TODO: fixe issue with Unrecognized fields for Journey messages
	    Map<String, JourneyStatus> status_map = new HashMap<>();
	    String journey_key = "";
	    //if input mapper can convert Journey Candidate, then 
	    //      1. Update JourneyStatus for journey key in domain audit to Candidate
	    try {
		    JourneyCandidateMessage journey_candidate_msg = input_mapper.readValue(target, JourneyCandidateMessage.class);
		    journey_key = journey_candidate_msg.getJourney().getKey();
		    
		    DomainAuditRecord domain_audit = (DomainAuditRecord)audit_record_service.findById(journey_candidate_msg.getDomainAuditRecordId()).get();

		    status_map = domain_audit.getJourneyStatusMap();
		    status_map.put(journey_key, JourneyStatus.READY);
		    
		    log.warn("journey candidate message deserialized");
		    log.warn("journey key retrieved : " + journey_key);
			return new ResponseEntity<String>("Successfully saved updated audit record", HttpStatus.OK);

	    }
	    catch(Exception e) {
	    	log.warn("error converting json string to JourneyCandidateMessage : "+e.getMessage());
	    	//e.printStackTrace();
	    }
	    
	    //if input mapper can convert Journey Verified, then 
	    //		1.  Update JourneyStatus for journey key in domain audit to Verified
	    try {
		    VerifiedJourneyMessage verified_journey_msg = input_mapper.readValue(target, VerifiedJourneyMessage.class);
		    journey_key = verified_journey_msg.getJourney().getKey();
		    
		    DomainAuditRecord domain_audit = (DomainAuditRecord)audit_record_service.findById(verified_journey_msg.getDomainAuditRecordId()).get();

		    status_map = domain_audit.getJourneyStatusMap();
		    status_map.put(journey_key, JourneyStatus.EXAMINED);
		    log.warn("verified journey message deserialized");
		    log.warn("journey key retrieved : " + journey_key);
			return new ResponseEntity<String>("Successfully saved updated audit record", HttpStatus.OK);
	    }
	    catch(Exception e) {
	    	log.warn("error converting json string to VerifiedJourneyMessage : "+e.getMessage());
	    }
	    
	    //if input mapper can convert Journey Verified, then 
	    //		1. 
	    //      2. Update JourneyStatus for journey key in domain audit to Verified
	    try {
		    DiscardedJourneyMessage discarded_journey_msg = input_mapper.readValue(target, DiscardedJourneyMessage.class);
		    journey_key = discarded_journey_msg.getJourney().getKey();

		    DomainAuditRecord domain_audit = (DomainAuditRecord)audit_record_service.findById(discarded_journey_msg.getDomainAuditRecordId()).get();

		    status_map = domain_audit.getJourneyStatusMap();
		    status_map.put(journey_key, JourneyStatus.DISCARDED);
		    log.warn("Discarded journey message deserialized");
		    log.warn("journey key retrieved : " + journey_key);
			return new ResponseEntity<String>("Successfully saved updated audit record", HttpStatus.OK);

	    }
	    catch(Exception e) {
	    	log.warn("error converting json string to DiscardedJourneyMessage : "+e.getMessage());
	    }
	    
	    
	    // update data extraction of domain audit to equal the number of journey keys that do NOT have an empty string associated with them over the
	    //       number of journeys present in domain audit
	    
	    //if domain audit is complete, then send email to user informing them of it's completion		
		
	    log.warn("journey key retrieved : " + journey_key);
		return new ResponseEntity<String>("Error occurred while updated audit progress", HttpStatus.BAD_REQUEST);
  }


	private double calculateProgress(AuditCategory aesthetics, 
									 int page_count, 
									 Set<Audit> audit_list, 
									 List<AuditName> audit_labels) {
		
		return 0.0;
		
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