package com.looksee.auditService.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.looksee.services.MessageBroadcaster;
import com.pusher.rest.Pusher;

@Configuration
public class ServiceConfiguration {
    
    @Bean
    public MessageBroadcaster messageBroadcaster(
            @Value("${pusher.appId}") String appId,
            @Value("${pusher.key}") String key,
            @Value("${pusher.secret}") String secret,
            @Value("${pusher.cluster}") String cluster) {
        
        Pusher pusher = new Pusher(appId, key, secret);
        pusher.setCluster(cluster);
        return new MessageBroadcaster(pusher);
    }
} 