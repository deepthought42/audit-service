package com.looksee.auditService.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.looksee.models.dto.AuditUpdateDto;
import com.looksee.services.MessageBroadcaster;

@Configuration
public class ServiceConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(ServiceConfiguration.class);
    
    @Bean
    @ConditionalOnMissingBean(MessageBroadcaster.class)
    public MessageBroadcaster messageBroadcaster() {
        // Mock implementation that doesn't require any Pusher configuration
        return new MessageBroadcaster() {
            @Override
            public void sendAuditUpdate(String auditId, AuditUpdateDto auditUpdate) {
                log.info("Mock MessageBroadcaster: Would send audit update for auditId={}, status={}", 
                        auditId, auditUpdate != null ? auditUpdate.getStatus() : "null");
                // No-op implementation for testing/development
            }
        };
    }
} 