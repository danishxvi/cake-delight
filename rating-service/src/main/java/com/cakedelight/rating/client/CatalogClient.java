package com.cakedelight.rating.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Synchronous client for cake-catalog-service, used to check that a rated
 * cake exists. Transient failures are retried with backoff; a 404 is a
 * definitive answer and is not retried.
 */
@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(@LoadBalanced RestClient.Builder restClientBuilder,
                         @Value("${catalog.service-url:http://cake-catalog-service}") String catalogServiceUrl) {
        this.restClient = restClientBuilder.baseUrl(catalogServiceUrl).build();
    }

    @Retryable(includes = {ResourceAccessException.class, HttpServerErrorException.class, IllegalStateException.class},
            maxRetries = 2, delay = 300, multiplier = 2)
    public boolean cakeExists(Long cakeId) {
        try {
            restClient.get().uri("/api/catalog/{id}", cakeId).retrieve().toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }
}
