package com.scrapper.config;

import java.time.Duration;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfig {
    @Value("${scraping.timeout:30000}")
    private int timeout;

    @Value("${http.headers.user-agent}")
    private String userAgent;

    @Bean
    public OkHttpClient httpClient() {
        return new OkHttpClient.Builder()
            .connectTimeout(Duration.ofMillis(timeout))
            .readTimeout(Duration.ofMillis(timeout))
            .writeTimeout(Duration.ofMillis(timeout))
            .addInterceptor(chain -> chain.proceed(
                chain.request().newBuilder()
                    .addHeader("User-Agent", userAgent)
                    .build()
            ))
            .build();
    }
}

