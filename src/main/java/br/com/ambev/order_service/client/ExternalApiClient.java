package br.com.ambev.order_service.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import br.com.ambev.order_service.dto.OrderDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@FeignClient(name = "external-api", url = "https://webhook.site/9999f3cc-aac8-4a13-8616-d5037a186d96", fallback = ExternalApiClientFallback.class) // Incluí essa url do webhook site para simular uma api externa
public interface ExternalApiClient {

    @Retry(name = "external-api")
    @CircuitBreaker(name = "external-api")
    @PostMapping("/orders/batch")
    void sendOrders(@RequestBody List<OrderDTO> orders);
     
}
