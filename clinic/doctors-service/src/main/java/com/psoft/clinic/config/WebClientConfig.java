package com.psoft.clinic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;

@Configuration
public class WebClientConfig {

    @Value("${internal.api.key}")
    private String internalApiKey;

    @PostConstruct
    public void init() {
        System.out.println("🔑 [WebClientConfig] internal.api.key = " + internalApiKey);
    }

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        System.out.println("🌐 [WebClientConfig] A configurar WebClient com X-INTERNAL-KEY...");
        return builder
                .defaultHeader("X-INTERNAL-KEY", internalApiKey)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                        .build())
                .build();
    }
}
