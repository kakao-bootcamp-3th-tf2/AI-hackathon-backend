package jojo.jjdc.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        CloseableHttpClient httpClient = HttpClients.custom()
                .useSystemProperties()
                .build();
        HttpClient client = httpClient;
        return builder
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client))
                .build();
    }
}
