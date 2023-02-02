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
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.looksee.auditService.mapper.Body;
import com.looksee.auditService.models.Account;
import com.looksee.auditService.models.Audit;
import com.looksee.auditService.models.AuditProgressUpdate;
import com.looksee.auditService.models.PageAuditRecord;
import com.looksee.auditService.models.PageState;
import com.looksee.auditService.models.enums.AuditCategory;
import com.looksee.auditService.models.enums.ExecutionStatus;
import com.looksee.auditService.models.message.DiscardedJourneyMessage;
import com.looksee.auditService.models.message.JourneyCandidateMessage;
import com.looksee.auditService.models.message.VerifiedJourneyMessage;
import com.looksee.auditService.services.AccountService;
import com.looksee.auditService.services.AuditRecordService;
import com.looksee.auditService.services.AuditService;
import com.looksee.auditService.services.SendGridMailService;
import com.looksee.utils.AuditUtils;

// PubsubController consumes a Pub/Sub message.
@RestController
public class AuditController {
	private static Logger log = LoggerFactory.getLogger(AuditController.class);

	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private SendGridMailService mail_service;
	
	@Autowired
	private AccountService account_service;
	
	
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body) throws JsonMappingException, JsonProcessingException, ExecutionException, InterruptedException {

		Body.Message message = body.getMessage();
		String data = message.getData();
	    String target = !data.isEmpty() ? new String(Base64.getDecoder().decode(data)) : "";
        log.warn("page audit msg received = "+target);

	    ObjectMapper input_mapper = new ObjectMapper();
	    
	    JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

	    //if message is audit message then update page audit
	    try {
		    AuditProgressUpdate audit_msg = input_mapper.readValue(target, AuditProgressUpdate.class);
			//update audit record
			PageAuditRecord audit_record = (PageAuditRecord)audit_record_service.findById(audit_msg.getPageAuditId()).get();
			
			Audit audit = audit_service.save(audit_msg.getAudit());
			audit_record_service.addAudit( audit_record.getId(), audit.getId() );
			
			if(AuditCategory.AESTHETICS.equals(audit.getCategory())) {
				audit_record.setAestheticAuditProgress(audit_msg.getProgress());
			}
			if(AuditCategory.CONTENT.equals(audit.getCategory())) {
				audit_record.setContentAuditProgress(audit_msg.getProgress());
			}
			if(AuditCategory.INFORMATION_ARCHITECTURE.equals(audit.getCategory())) {
				audit_record.setInfoArchitectureAuditProgress(audit_msg.getProgress());
			}
			
			boolean is_page_audit_complete = AuditUtils.isPageAuditComplete(audit_record);
			
			if(is_page_audit_complete) {
				audit_record.setEndTime(LocalDateTime.now());
				audit_record.setStatus(ExecutionStatus.COMPLETE);
			
				PageState page = audit_record_service.getPageStateForAuditRecord(audit_record.getId());								
				Account account = account_service.findById(audit_msg.getAccountId()).get();
				
				log.warn("sending email to account :: "+account.getEmail());
				mail_service.sendPageAuditCompleteEmail(account.getEmail(), page.getUrl(), audit_record.getId());
			}
			
			audit_record = (PageAuditRecord)audit_record_service.save(audit_record);	
			
			//TODO : publish Pusher Domain Audit update
			MessageBroadcaster.sendAuditRecord(audit_msg.getAccountId()+"", null);
	    }
	    catch(Exception e) {
	    	log.warn("An exception occurred while converting JSON to AuditProgressUpdate");
	    }
	    
	    String journey_key = "";
	    //if input mapper can convert Journey Candidate, then 
	    //      1. Update JourneyStatus for journey key in domain audit to Candidate
	    try {
		    JourneyCandidateMessage journey_candidate_msg = input_mapper.readValue(target, JourneyCandidateMessage.class);
		    journey_key = journey_candidate_msg.getJourney().getKey();
	    }
	    catch(Exception e) {
	    	log.warn("error converting json string to JourneyCandidateMessage");
	    }
	    
	    //if input mapper can convert Journey Verified, then 
	    //		1.  Update JourneyStatus for journey key in domain audit to Verified
	    try {
		    VerifiedJourneyMessage verified_journey_msg = input_mapper.readValue(target, VerifiedJourneyMessage.class);
		    journey_key = verified_journey_msg.getJourney().getKey();
	    }
	    catch(Exception e) {
	    	log.warn("error converting json string to VerifiedJourneyMessage");
	    }
	    
	    //if input mapper can convert Journey Verified, then 
	    //		1. 
	    //      2. Update JourneyStatus for journey key in domain audit to Verified
	    try {
		    DiscardedJourneyMessage discarded_journey_msg = input_mapper.readValue(target, DiscardedJourneyMessage.class);
		    journey_key = discarded_journey_msg.getJourney().getKey();
	    }
	    catch(Exception e) {
	    	log.warn("error converting json string to VerifiedJourneyMessage");
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