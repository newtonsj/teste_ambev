package br.com.ambev.order_service.service.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.ambev.order_service.dto.OrderDTO;
import br.com.ambev.order_service.service.OrderService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderConsumer {

	private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public OrderConsumer(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "orders", groupId = "order-group")
    public void consumeMessage(String message) {
        try {
            OrderDTO orderDTO = objectMapper.readValue(message, OrderDTO.class);
            orderService.createOrder(orderDTO);
        } catch (Exception e) {
            log.error("Erro ao processar pedido {}", message, e.getMessage());
        }
    }
}
