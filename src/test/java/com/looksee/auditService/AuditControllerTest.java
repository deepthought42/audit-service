package com.looksee.auditService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.looksee.mapper.Body;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.DomainService;
import com.looksee.services.MessageBroadcaster;
import com.looksee.services.PageStateService;

class AuditControllerTest {
    private AuditController controller;
    private AuditRecordService auditRecordService;
    private PageStateService pageStateService;

    @BeforeEach
    void setUp() {
        controller = new AuditController();
        auditRecordService = mock(AuditRecordService.class);
        pageStateService = mock(PageStateService.class);

        ReflectionTestUtils.setField(controller, "audit_record_service", auditRecordService);
        ReflectionTestUtils.setField(controller, "page_state_service", pageStateService);
        ReflectionTestUtils.setField(controller, "account_service", mock(AccountService.class));
        ReflectionTestUtils.setField(controller, "domain_service", mock(DomainService.class));
        ReflectionTestUtils.setField(controller, "messageBroadcaster", mock(MessageBroadcaster.class));
    }

    @Test
    void receiveMessageReturnsBadRequestForNullBody() throws Exception {
        ResponseEntity<String> response = controller.receiveMessage(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid message payload", response.getBody());
    }

    @Test
    void receiveMessageReturnsBadRequestForInvalidBase64Data() throws Exception {
        Body.Message message = mock(Body.Message.class);
        Body body = mock(Body.class);
        when(body.getMessage()).thenReturn(message);
        when(message.getData()).thenReturn("$$$not_base64$$$");

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid message payload", response.getBody());
    }

    @Test
    void receiveMessageReturnsBadRequestWhenPayloadDoesNotMatchKnownMessageTypes() throws Exception {
        Body.Message message = mock(Body.Message.class);
        Body body = mock(Body.class);
        when(body.getMessage()).thenReturn(message);
        when(message.getData()).thenReturn(Base64.getEncoder().encodeToString("{}".getBytes()));

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error occurred while updating audit progress", response.getBody());
    }

    @Test
    void getPageDataExtractionProgressReturnsOneWhenAuditsExist() {
        long auditRecordId = 10L;
        when(auditRecordService.getAllAudits(auditRecordId)).thenReturn(Set.of(mock(Audit.class)));

        double progress = ReflectionTestUtils.invokeMethod(controller, "getPageDataExtractionProgress", auditRecordId);

        assertEquals(1.0, progress);
    }

    @Test
    void getPageDataExtractionProgressReturnsZeroWhenNoPageFound() {
        long auditRecordId = 11L;
        when(auditRecordService.getAllAudits(auditRecordId)).thenReturn(Collections.emptySet());
        when(auditRecordService.findPage(auditRecordId)).thenReturn(null);

        double progress = ReflectionTestUtils.invokeMethod(controller, "getPageDataExtractionProgress", auditRecordId);

        assertEquals(0.0, progress);
    }

    @Test
    void getPageDataExtractionProgressCalculatesMilestoneProgress() {
        long auditRecordId = 12L;
        PageState pageState = mock(PageState.class);

        when(auditRecordService.getAllAudits(auditRecordId)).thenReturn(Collections.emptySet());
        when(auditRecordService.findPage(auditRecordId)).thenReturn(pageState);
        when(pageState.getId()).thenReturn(77L);
        when(pageStateService.getElementStateCount(77L)).thenReturn(500);

        double progress = ReflectionTestUtils.invokeMethod(controller, "getPageDataExtractionProgress", auditRecordId);

        assertEquals(1.5, progress);
    }

    @Test
    void getDomainDataExtractionProgressReturnsMinimumWhenTotalJourneysTooLow() {
        DomainAuditRecord domainAudit = mock(DomainAuditRecord.class);
        when(domainAudit.getId()).thenReturn(25L);
        when(auditRecordService.getNumberOfJourneysWithStatus(25L, JourneyStatus.CANDIDATE)).thenReturn(1);
        when(auditRecordService.getNumberOfJourneys(25L)).thenReturn(1);

        double progress = ReflectionTestUtils.invokeMethod(controller, "getDomainDataExtractionProgress", domainAudit);

        assertEquals(0.01, progress);
    }

    @Test
    void getDomainDataExtractionProgressReturnsRatioForJourneys() {
        DomainAuditRecord domainAudit = mock(DomainAuditRecord.class);
        when(domainAudit.getId()).thenReturn(26L);
        when(auditRecordService.getNumberOfJourneysWithStatus(26L, JourneyStatus.CANDIDATE)).thenReturn(2);
        when(auditRecordService.getNumberOfJourneys(26L)).thenReturn(5);

        double progress = ReflectionTestUtils.invokeMethod(controller, "getDomainDataExtractionProgress", domainAudit);

        assertEquals(0.6, progress);
    }

    @Test
    void buildDomainAuditRecordDTOThrowsWhenRecordMissing() {
        when(auditRecordService.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(controller, "buildDomainAuditRecordDTO", 99L));
    }

    @Test
    void buildDomainAuditRecordDTOThrowsWhenRecordIsNotDomainAudit() {
        AuditRecord nonDomainRecord = mock(AuditRecord.class);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(nonDomainRecord));

        assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(controller, "buildDomainAuditRecordDTO", 100L));
    }

    @Test
    void buildDomainAuditRecordDTOReturnsUpdateForDomainAudit() {
        DomainAuditRecord domainAudit = mock(DomainAuditRecord.class);
        PageAuditRecord pageAudit = mock(PageAuditRecord.class);

        when(domainAudit.getId()).thenReturn(101L);
        when(domainAudit.getAuditLabels()).thenReturn(Collections.emptySet());
        when(pageAudit.getId()).thenReturn(202L);

        when(auditRecordService.findById(101L)).thenReturn(Optional.of(domainAudit));
        when(auditRecordService.getAllPageAudits(101L)).thenReturn(Set.of(pageAudit));
        when(auditRecordService.getAllAuditsForPageAuditRecord(202L)).thenReturn(Collections.emptySet());
        when(auditRecordService.getNumberOfJourneysWithStatus(101L, JourneyStatus.CANDIDATE)).thenReturn(0);
        when(auditRecordService.getNumberOfJourneys(101L)).thenReturn(2);

        Object dto = ReflectionTestUtils.invokeMethod(controller, "buildDomainAuditRecordDTO", 101L);

        assertEquals("AuditUpdateDto", dto.getClass().getSimpleName());
    }
}
