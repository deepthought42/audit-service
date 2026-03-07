package com.looksee.auditService;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.mapper.Body;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.DomainService;
import com.looksee.services.MessageBroadcaster;
import com.looksee.services.PageStateService;

class AuditControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuditController controller;

    @BeforeEach
    void setUp() {
        controller = new AuditController();

        AuditRecordService auditRecordService = Mockito.mock(AuditRecordService.class);
        AccountService accountService = Mockito.mock(AccountService.class);
        DomainService domainService = Mockito.mock(DomainService.class);
        PageStateService pageStateService = Mockito.mock(PageStateService.class);
        MessageBroadcaster messageBroadcaster = Mockito.mock(MessageBroadcaster.class);

        ReflectionTestUtils.setField(controller, "audit_record_service", auditRecordService);
        ReflectionTestUtils.setField(controller, "account_service", accountService);
        ReflectionTestUtils.setField(controller, "domain_service", domainService);
        ReflectionTestUtils.setField(controller, "page_state_service", pageStateService);
        ReflectionTestUtils.setField(controller, "messageBroadcaster", messageBroadcaster);
    }

    @Test
    void receiveMessage_shouldReturnBadRequest_whenBodyIsNull() throws Exception {
        ResponseEntity<String> response = controller.receiveMessage(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid message payload", response.getBody());
    }

    @Test
    void receiveMessage_shouldReturnBadRequest_whenBase64PayloadIsInvalid() throws Exception {
        Body body = objectMapper.readValue("{\"message\":{\"data\":\"###not-base64###\"}}", Body.class);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid message payload", response.getBody());
    }

    @Test
    void receiveMessage_shouldReturnBadRequest_whenMessageTypeIsUnknown() throws Exception {
        String encodedUnknownPayload = Base64.getEncoder().encodeToString("{}".getBytes(StandardCharsets.UTF_8));
        Body body = objectMapper.readValue(
            "{\"message\":{\"data\":\"" + encodedUnknownPayload + "\"}}",
            Body.class
        );

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error occurred while updating audit progress", response.getBody());
    }
}
