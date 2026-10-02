package com.cakedelight.rating.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * REST clients for calls to other services. {@code @EnableResilientMethods}
 * activates {@code @Retryable}.
 */
@Configuration
@EnableResilientMethods
public class RestClientConfig {

    /**
     * Plain builder for everything that calls a real host name, including the
     * Eureka client's own calls to the registry. Without it, Eureka would pick
     * up the load-balanced builder below and fail to reach eureka-service.
     */
    @Bean
    @Primary
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    /**
     * Load-balanced builder: URLs use the Eureka service name
     * (http://cake-catalog-service) instead of a fixed host. Inject it with
     * {@code @LoadBalanced}.
     */
    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // Three attempts at most (see CatalogClient), which stays well inside
        // the gateway's 30 second response timeout.
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().requestFactory(requestFactory);
    }
}
