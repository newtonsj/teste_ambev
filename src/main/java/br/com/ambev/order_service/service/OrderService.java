package br.com.ambev.order_service.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.ambev.order_service.dto.OrderDTO;
import br.com.ambev.order_service.dto.ProductDTO;
import br.com.ambev.order_service.model.Order;
import br.com.ambev.order_service.model.OrderStatus;
import br.com.ambev.order_service.model.Product;
import br.com.ambev.order_service.repository.OrderRepository;
import br.com.ambev.order_service.service.producer.OrderProducer;

@Service
public class OrderService {

	private final OrderRepository orderRepository;	
	private final OrderProducer orderProducer;
	private final ObjectMapper objectMapper;

	
	 public OrderService(OrderRepository orderRepository, OrderProducer orderProducer, ObjectMapper objectMapper) {
	        this.orderRepository = orderRepository;
	        this.orderProducer = orderProducer;
	        this.objectMapper = objectMapper;
	    }

	    
	    public OrderDTO receiveOrder(OrderDTO orderDTO) {
	    	
	        if (orderRepository.findByExternalId(orderDTO.getExternalId().toString()).isPresent()) {
	            throw new IllegalArgumentException("Pedido duplicado: " + orderDTO.getExternalId());
	        }
	        
	        try {
	        	
	        	String message = objectMapper.writeValueAsString(orderDTO);
	            orderProducer.sendMessage(message);
	        }catch (Exception e) {
	        	throw new IllegalArgumentException("Erro ao criar pedido: " + orderDTO.getExternalId());
			}
            
	        return orderDTO;
	     
	    }
	    
	    @Transactional
	    public void createOrder(OrderDTO orderDTO) {

	        orderDtoToOrder(orderDTO);
	        
	        Order order = orderDtoToOrder(orderDTO);
	        order.getProducts().forEach(product -> product.setOrder(order));
	        
	        BigDecimal totalPrice = order.getProducts().stream()
	                .map(product -> product.getPrice().multiply(BigDecimal.valueOf(product.getQuantity())))
	                .reduce(BigDecimal.ZERO, BigDecimal::add);
	        
	        order.setTotalPrice(totalPrice);
	        order.setStatus(OrderStatus.RECEIVED);
	        
	        orderRepository.save(order);
	    }

	    public void processOrder(Long orderId) {
	        Order order = orderRepository.findById(orderId)
	                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + orderId));

	        order.setStatus(OrderStatus.PROCESSED);
	        orderRepository.save(order);
	    }
	    
	    public Page<OrderDTO> getOrders(Pageable pageable) {	    	
	        Page<Order> orders = orderRepository.findAll(pageable);
	        return orders.map(this::toOrderDTO);
	    }

		
		
	    public OrderDTO toOrderDTO(Order order) {
	    	
	    	
	        List<ProductDTO> productDTOs = order.getProducts().stream()
	            .map(product -> ProductDTO.builder()
	            		.name(product.getName())
	            		.price(product.getPrice())
	            		.description(product.getDescription())
	            		.quantity(product.getQuantity()).build())
	            .collect(Collectors.toList());
	        
	        return OrderDTO.builder()
	        		.id(order.getId())
	    	        .externalId(UUID.fromString(order.getExternalId()))
	    	        .products(productDTOs)
	    	        .totalPrice(order.getTotalPrice())
	    	        .build();
	    }
	    
	    public Order orderDtoToOrder(OrderDTO orderDTO) {
	    	
	        List<Product> products = orderDTO.getProducts().stream()
	            .map(productDTO -> Product.builder()
	                .name(productDTO.getName())
	                .price(productDTO.getPrice())
	                .description(productDTO.getDescription())
	                .quantity(productDTO.getQuantity()) 
	                .build())
	            .collect(Collectors.toList());

	        return Order.builder()
	            .externalId(orderDTO.getExternalId().toString())
	            .products(products)
	            .totalPrice(orderDTO.getTotalPrice())
	            .build();
	    }
}
