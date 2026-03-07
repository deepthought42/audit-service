package com.looksee.auditService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.looksee.mapper.Body;

class AuditControllerTest {

    private final AuditController auditController = new AuditController();

    @Test
    void receiveMessageShouldReturnBadRequestForNullBody() throws Exception {
        ResponseEntity<String> response = auditController.receiveMessage(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid message payload", response.getBody());
    }

    @Test
    void receiveMessageShouldReturnBadRequestForInvalidBase64Payload() throws Exception {
        Body body = mock(Body.class);
        Body.Message message = mock(Body.Message.class);

        when(body.getMessage()).thenReturn(message);
        when(message.getData()).thenReturn("not-base64$payload");

        ResponseEntity<String> response = auditController.receiveMessage(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid message payload", response.getBody());
    }

    @Test
    void receiveMessageShouldReturnBadRequestForUnknownMessageType() throws Exception {
        Body body = mock(Body.class);
        Body.Message message = mock(Body.Message.class);
        String unknownJson = "{\"unexpected\":\"message\"}";
        String encodedPayload = java.util.Base64.getEncoder().encodeToString(unknownJson.getBytes());

        when(body.getMessage()).thenReturn(message);
        when(message.getData()).thenReturn(encodedPayload);

        ResponseEntity<String> response = auditController.receiveMessage(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Error occurred while updating audit progress"));
    }
}
