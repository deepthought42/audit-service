package com.looksee.auditService.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfiguration {
    
    /**
     * Provides mock Pusher configuration for development/testing.
     * This allows MessageBroadcaster to be created without real Pusher credentials.
     */
    @Bean
    @ConfigurationProperties(prefix = "pusher.mock")
    public PusherConfig pusherConfig() {
        PusherConfig config = new PusherConfig();
        config.setAppId("mock-app-id");
        config.setKey("mock-key");
        config.setSecret("mock-secret");
        config.setCluster("mock-cluster");
        return config;
    }
    
    public static class PusherConfig {
        private String appId;
        private String key;
        private String secret;
        private String cluster;
        
        // Getters and setters
        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public String getCluster() { return cluster; }
        public void setCluster(String cluster) { this.cluster = cluster; }
    }
} 