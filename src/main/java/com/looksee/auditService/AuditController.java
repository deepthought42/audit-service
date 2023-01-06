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
import com.looksee.auditService.gcp.PubSubAuditUpdatePublisherImpl;
import com.looksee.auditService.mapper.Body;
import com.looksee.auditService.models.Account;
import com.looksee.auditService.models.Audit;
import com.looksee.auditService.models.AuditRecord;
import com.looksee.auditService.models.PageState;
import com.looksee.auditService.models.enums.AuditCategory;
import com.looksee.auditService.models.enums.ExecutionStatus;
import com.looksee.auditService.models.message.AuditMessage;
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
	
	@Autowired
	private PubSubAuditUpdatePublisherImpl audit_update_topic;
	
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity receiveMessage(@RequestBody Body body) throws JsonMappingException, JsonProcessingException, ExecutionException, InterruptedException {
		log.warn("body :: "+body);
		// Get PubSub message from request body.
		Body.Message message = body.getMessage();
		log.warn("message " + message);

		String data = message.getData();
		log.warn("data :: "+data);
		//retrieve audit record and determine type of audit record
    
		byte[] decodedBytes = Base64.getUrlDecoder().decode(data);
		String decoded_json = new String(decodedBytes);

		//create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
    
		//convert json string to object
		AuditMessage audit_msg = objectMapper.readValue(decoded_json, AuditMessage.class);
	    
		//update audit record
		AuditRecord audit_record = audit_record_service.findById(audit_msg.getPageAuditRecordId()).get();
		
		Audit audit = audit_msg.getAudit();
		
		audit = audit_service.save(audit);
		audit_record_service.addAudit( audit_record.getId(), audit.getId() );
		
		if(AuditCategory.AESTHETICS.equals(audit.getCategory())) {
			audit_record.setAestheticAuditProgress(audit_msg.getAuditProgress());
		}
		if(AuditCategory.CONTENT.equals(audit.getCategory())) {
			audit_record.setContentAuditProgress(audit_msg.getAuditProgress());
		}
		if(AuditCategory.INFORMATION_ARCHITECTURE.equals(audit.getCategory())) {
			audit_record.setInfoArchitectureAuditProgress(audit_msg.getAuditProgress());
		}
		
		audit_record_service.save(audit_record);
		
		boolean is_page_audit_complete = AuditUtils.isPageAuditComplete(audit_record);						
		if(is_page_audit_complete) {
			audit_record.setEndTime(LocalDateTime.now());
			audit_record.setStatus(ExecutionStatus.COMPLETE);
			audit_record = audit_record_service.save(audit_record);	
		
			PageState page = audit_record_service.getPageStateForAuditRecord(audit_record.getId());								
			Account account = account_service.findById(audit_msg.getAccountId()).get();
			
			log.warn("sending email to account :: "+account.getEmail());
			mail_service.sendPageAuditCompleteEmail(account.getEmail(), page.getUrl(), audit_record.getId());
		}
		
		return new ResponseEntity("Successfully sent message to audit manager", HttpStatus.OK);
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