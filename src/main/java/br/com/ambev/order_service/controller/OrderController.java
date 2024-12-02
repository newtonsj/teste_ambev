package br.com.ambev.order_service.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.ambev.order_service.dto.OrderDTO;
import br.com.ambev.order_service.model.Order;
import br.com.ambev.order_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "APIs para gerenciar pedidos")
public class OrderController {
	
	private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Cria um pedido", description = "Adiciona um novo pedido")
    public ResponseEntity<OrderDTO> createOrder(@RequestBody OrderDTO orderDTO) {
        return ResponseEntity.ok(orderService.receiveOrder(orderDTO));
    }
    

    @GetMapping
    @Operation(summary = "Lista pedidos", description = "Retorna uma lista paginada de pedidos")
    public Page<OrderDTO> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return orderService.getOrders(pageable);
    }

    @PutMapping("/{id}/process")
    public ResponseEntity<Void> processOrder(@PathVariable Long id) {
        orderService.processOrder(id);
        return ResponseEntity.noContent().build();
    }

}
