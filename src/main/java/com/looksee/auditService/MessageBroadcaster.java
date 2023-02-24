package com.looksee.auditService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.looksee.auditService.models.AuditRecord;
import com.looksee.auditService.models.LookseeObject;
import com.looksee.auditService.models.dto.AuditUpdateDto;
import com.pusher.rest.Pusher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines methods for emitting data to subscribed clients
 */
public class MessageBroadcaster {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(MessageBroadcaster.class);
	
	private static Pusher pusher = new Pusher("1149966", "c88f4e4c6e128ed219c2", "149f5a3cb7f7c8d7205b");
	
	static{
		pusher.setCluster("us2");
		pusher.setEncrypted(true);
	}
	
	/**
     * Message emitter that sends {@link Test} to all registered clients
     * 
     * @param test {@link Test} to be emitted to clients
     * @throws JsonProcessingException 
     */
	public static void broadcastPathObject(LookseeObject path_object, String host, String user_id) throws JsonProcessingException {	
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        //Object to JSON in String
        String path_object_json = mapper.writeValueAsString(path_object);
        
		pusher.trigger(user_id+host, "path_object", path_object_json);
	}
	
	/**
	 * send {@link AuditRecord} to the users pusher channel
	 * @param account_id
	 * @param audit
	 */
	public static void sendAuditUpdate(String account_id, AuditUpdateDto audit_update) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

		String audit_update_str = mapper.writeValueAsString(audit_update);
		pusher.trigger(account_id, "audit-update", audit_update_str);
	}
}
