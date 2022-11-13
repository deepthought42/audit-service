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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.looksee.audit_service.mapper.Body;

// PubsubController consumes a Pub/Sub message.
@RestController
public class AuditController {
	
	@Autowired
	private AuditRecordService audit_record_service;
	
  @RequestMapping(value = "/", method = RequestMethod.POST)
  public ResponseEntity receiveMessage(@RequestBody Body body) {
    // Get PubSub message from request body.
    Body.Message message = body.getMessage();
    if (message == null) {
      String msg = "Bad Request: invalid Pub/Sub message format";
      System.out.println(msg);
      return new ResponseEntity(msg, HttpStatus.BAD_REQUEST);
    }

    AuditRecord data = message.getData();
    
  //retrieve audit record and determine type of audit record
    AuditRecord audit_record = audit_record_service.findById(data.getId()).get();
    if(audit_record == null) {
    	//TODO: SEND PUB SUB MESSAGE THAT AUDIT RECORD NOT FOUND WITH PAGE DATA EXTRACTION MESSAGE
    }
    else {
    	//TODO: SEND PUB SUB MESSAGE THAT AUDIT RECORD NOT FOUND WITH PAGE DATA EXTRACTION MESSAGE

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
}
// [END run_pubsub_handler]
// [END cloudrun_pubsub_handler]