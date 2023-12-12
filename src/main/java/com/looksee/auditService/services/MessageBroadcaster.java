package com.looksee.auditService.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.looksee.auditService.models.dto.AuditUpdateDto;
import com.looksee.auditService.models.PageAuditRecord;

import com.pusher.rest.Pusher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Defines methods for emitting data to subscribed clients
 */
@Service
public class MessageBroadcaster {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(MessageBroadcaster.class);
	
	private Pusher pusher;
	
	public MessageBroadcaster(@Value( "${pusher.appId}" ) String app_id,
			@Value( "${pusher.key}" ) String key,
			@Value( "${pusher.secret}" ) String secret,
			@Value("${pusher.cluster}") String cluster) {
		pusher = new Pusher(app_id, key, secret);
		pusher.setCluster(cluster);
		pusher.setEncrypted(true);
	}
	
	/**
	 * Sends {@linkplain PageAuditRecord} to user via Pusher
	 * @param account_id
	 * @param audit_record
	 * @throws JsonProcessingException
	 */
	public void sendAuditUpdate(String channel_id, AuditUpdateDto audit_update) throws JsonProcessingException {
		assert channel_id != null;
		assert !channel_id.isEmpty();
		assert audit_update != null;
		
		ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String audit_record_json = mapper.writeValueAsString(audit_update);
        log.warn("Sending page audit record to user:    "+audit_record_json);
		pusher.trigger(channel_id, "audit-progress", audit_record_json);
	}
}
