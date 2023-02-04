package com.looksee.auditService;

import java.time.LocalDateTime;
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
import com.looksee.auditService.mapper.Body;
import com.looksee.auditService.models.Account;
import com.looksee.auditService.models.Domain;
import com.looksee.auditService.models.DomainAuditRecord;
import com.looksee.auditService.models.PageAuditRecord;
import com.looksee.auditService.models.enums.AuditCategory;
import com.looksee.auditService.models.enums.ExecutionStatus;
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
	    
	    //JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
	    
	    //if message is audit message then update page audit
	    try {
		    AuditProgressUpdate audit_msg = input_mapper.readValue(target, AuditProgressUpdate.class);
			//update audit record
			PageAuditRecord audit_record = (PageAuditRecord)audit_record_service.findById(audit_msg.getPageAuditId()).get();
			
			if(AuditCategory.AESTHETICS.equals(audit_msg.getCategory())) {
				audit_record.setAestheticAuditProgress(audit_msg.getProgress());
			}
			if(AuditCategory.CONTENT.equals(audit_msg.getCategory())) {
				audit_record.setContentAuditProgress(audit_msg.getProgress());
			}
			if(AuditCategory.INFORMATION_ARCHITECTURE.equals(audit_msg.getCategory())) {
				audit_record.setInfoArchitectureAuditProgress(audit_msg.getProgress());
			}
			
			//if page audit is complete then 
			boolean is_page_audit_complete = AuditUtils.isPageAuditComplete(audit_record);
			
			if(is_page_audit_complete) {
				audit_record.setEndTime(LocalDateTime.now());
				audit_record.setStatus(ExecutionStatus.COMPLETE);
				
				//TODO: move following logic to domain audit
				//PageState page = audit_record_service.getPageStateForAuditRecord(audit_record.getId());								
				//Account account = account_service.findById(audit_msg.getAccountId()).get();
				//log.warn("sending email to account :: "+account.getEmail());
				//mail_service.sendPageAuditCompleteEmail(account.getEmail(), page.getUrl(), audit_record.getId());
			}
			
			audit_record = (PageAuditRecord)audit_record_service.save(audit_record);	
			
			log.warn("sending audit record update to user");
			//TODO : publish Pusher Domain Audit update
			MessageBroadcaster.sendAuditRecord(audit_msg.getAccountId()+"", null);
			
		    //DomainAuditRecord domain_audit = (DomainAuditRecord)audit_record_service.findById(audit_msg.getDomainAuditRecordId()).get();
		    Set<PageAuditRecord> page_audits = audit_record_service.getAllPageAudits(audit_msg.getDomainAuditRecordId());
		    
			//if domain audit is complete then send email
			page_audits = page_audits.stream()
										.filter(audit -> audit.getAestheticAuditProgress() < 1.0)
										.filter(audit -> audit.getContentAuditProgress() < 1.0)
										.filter(audit -> audit.getInfoArchitechtureAuditProgress() < 1.0)
										.collect(Collectors.toSet());

			//if page audits is empty, then all audits are complete
			if(page_audits.isEmpty() ) {
			    DomainAuditRecord domain_audit = (DomainAuditRecord)audit_record_service.findById(audit_msg.getDomainAuditRecordId()).get();
			    if( domain_audit.getDataExtractionProgress() == 1 ){
			    	//send email that audit is complete
					Account account = account_service.findById(audit_msg.getAccountId()).get();
					Domain domain = domain_service.findById(audit_msg.getDomainId()).get();
					mail_service.sendDomainAuditCompleteEmail(account.getEmail(), 
															  domain.getUrl(), 
															  audit_msg.getDomainId());
			    }
			}
			
			return new ResponseEntity<String>("Successfully saved updated audit record", HttpStatus.OK);			
	    }
	    catch(Exception e) {
	    	log.warn("An exception occurred while converting JSON to AuditProgressUpdate : "+e.getMessage());
	    	//e.printStackTrace();
	    }
	    
	    //Get Domain Audit Record
	  
	    
	    
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
	    }
	    catch(Exception e) {
	    	log.warn("error converting json string to DiscardedJourneyMessage : "+e.getMessage());
	    }
	    
	    
	    log.warn("journey key retrieved : " + journey_key);
	    // update data extraction of domain audit to equal the number of journey keys that do NOT have an empty string associated with them over the
	    //       number of journeys present in domain audit
	    
	    //if domain audit is complete, then send email to user informing them of it's completion		
		
		return new ResponseEntity<String>("Successfully saved updated audit record", HttpStatus.OK);
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