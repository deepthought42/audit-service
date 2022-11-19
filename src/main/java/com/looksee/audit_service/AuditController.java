package com.looksee.audit_service;

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
import java.util.UUID;
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
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.looksee.audit_service.gcp.PubSubPublisherImpl;
import com.looksee.audit_service.mapper.Body;
import com.looksee.audit_service.model.AuditRecord;
import com.looksee.audit_service.model.PageAuditRecord;
import com.looksee.audit_service.model.dto.PageBuiltMessage;
import com.looksee.audit_service.models.enums.ExecutionStatus;
import com.looksee.audit_service.services.AuditRecordService;

// PubsubController consumes a Pub/Sub message.
@RestController
public class AuditController {
	private static Logger log = LoggerFactory.getLogger(AuditController.class);

	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private PubSubPublisherImpl pubSubPublisherImpl;
	
  @RequestMapping(value = "/", method = RequestMethod.POST)
  public ResponseEntity receiveMessage(@RequestBody Body body) throws JsonMappingException, JsonProcessingException, ExecutionException, InterruptedException {
	  log.warn("body :: "+body);
	  // Get PubSub message from request body.
    Body.Message message = body.getMessage();
    log.warn("message " + message);
    /*
    if (message == null) {
      String msg = "Bad Request: invalid Pub/Sub message format";
      System.out.println(msg);
      return new ResponseEntity(msg, HttpStatus.BAD_REQUEST);
    }
*/
    String data = message.getData();
    log.warn("data :: "+data);
  //retrieve audit record and determine type of audit record
    
    byte[] decodedBytes = Base64.getUrlDecoder().decode(data);
    String decoded_json = new String(decodedBytes);

    
    //create ObjectMapper instance
	ObjectMapper objectMapper = new ObjectMapper();
	
	//convert json string to object
	PageBuiltMessage audit_record_msg = objectMapper.readValue(decoded_json, PageBuiltMessage.class);
	log.warn("audit record = " + audit_record_msg);

	AuditRecord audit_record = new PageAuditRecord(ExecutionStatus.BUILDING_PAGE, true);
	
	audit_record = audit_record_service.save(audit_record);
	audit_record_service.addPageAuditToDomainAudit(audit_record_msg.getDomainAuditId(), audit_record.getId());
	
	audit_record_service.addPageToAuditRecord(audit_record.getId(), audit_record_msg.getPageId());
	
	Map<String, String> attributeMap = new HashMap<>();
	attributeMap.put("accountId", audit_record_msg.getAccountId());
	attributeMap.put("domainAuditId", audit_record_msg.getDomainAuditId()+"");
	attributeMap.put("pageAuditId", audit_record.getId()+"");
	attributeMap.put("pageId", audit_record_msg.getPageId()+"");
	
	/*
    PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
            .putAllAttributes(attributeMap)
            .setData(ByteString.copyFromUtf8("Audit record not found"))
            .setMessageId(UUID.randomUUID().toString())
            .build();
    */
	JsonMapper mapper = new JsonMapper().builder().addModule(new JavaTimeModule()).build();;
    String audit_record_json = mapper.writeValueAsString(attributeMap);
	System.out.println("audit record json = " + audit_record_json);
			
    log.warn("pub sub message = "+audit_record_json);
    if(audit_record == null) {
    	//TODO: SEND PUB SUB MESSAGE THAT AUDIT RECORD NOT FOUND WITH PAGE DATA EXTRACTION MESSAGE
    	pubSubPublisherImpl.publish(audit_record_json);
    }
    else {
    	//TODO: SEND PUB SUB MESSAGE THAT AUDIT RECORD NOT FOUND WITH PAGE DATA EXTRACTION MESSAGE
    	pubSubPublisherImpl.publish(audit_record_json);
    	
    	/*
		log.warn("Initiating page audit = "+audit_record.getId());
		ActorRef audit_manager = actor_system.actorOf(SpringExtProvider.get(actor_system)
	   												.props("singlePageAuditManager"), "singlePageAuditManager"+UUID.randomUUID());
		audit_manager.tell(message, ActorRef.noSender());
		*/
    }
    
    return new ResponseEntity("Successfully sent message to audit manager", HttpStatus.OK);
    
    /*
    String target =
        !StringUtils.isEmpty(data) ? new String(Base64.getDecoder().decode(data)) : "World";
    String msg = "Hello " + target + "!";

    System.out.println(msg);
    return new ResponseEntity(msg, HttpStatus.OK);
    */
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