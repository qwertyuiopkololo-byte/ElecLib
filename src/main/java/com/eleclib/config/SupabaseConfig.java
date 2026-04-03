package com.eleclib.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SupabaseConfig {

    @Bean
    @ConfigurationProperties(prefix = "supabase")
    public SupabaseProperties supabaseProperties() {
        return new SupabaseProperties();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @lombok.Data
    public static class SupabaseProperties {
        private String url;
        private String key;
    }
}
