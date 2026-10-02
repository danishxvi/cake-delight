package com.cakedelight.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false"})
class ApiGatewayApplicationTests {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void allServiceRoutesAreConfigured() {
        List<String> routeIds = routeLocator.getRoutes().map(Route::getId).collectList().block();

        assertThat(routeIds).contains("cake-catalog-service", "order-service", "rating-service", "notification-service");
    }
}
