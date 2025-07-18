package com.looksee.auditService.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfiguration {
    /*
    @Bean
    public MessageBroadcaster messageBroadcaster(
            @Value("${pusher.appId}") String appId,
            @Value("${pusher.key}") String key,
            @Value("${pusher.secret}") String secret,
            @Value("${pusher.cluster}") String cluster) {
        
        Pusher pusher = new Pusher(appId, key, secret);
        pusher.setCluster(cluster);
        return new MessageBroadcaster(pusher);
    }*/
} 