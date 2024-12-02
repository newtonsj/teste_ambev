package br.com.ambev.order_service;

import br.com.ambev.order_service.dto.OrderDTO;
import br.com.ambev.order_service.dto.ProductDTO;
import br.com.ambev.order_service.model.Order;
import br.com.ambev.order_service.model.OrderStatus;
import br.com.ambev.order_service.model.Product;
import br.com.ambev.order_service.repository.OrderRepository;
import br.com.ambev.order_service.service.OrderService;
import br.com.ambev.order_service.service.producer.OrderProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OrderServiceTest {

	@Mock
	private OrderRepository orderRepository;
	
	@Mock
	private OrderProducer orderProducer;
	
	@Mock
	private ObjectMapper objectMapper;
	
	@InjectMocks
	private OrderService orderService;
	
	@BeforeEach
	void setUp() {
	    MockitoAnnotations.openMocks(this);
	}
	
	@Test
	void testReceiveOrder_DuplicatedOrder_ShouldThrowException() {
	    UUID externalId = UUID.randomUUID();
	    OrderDTO orderDTO = OrderDTO.builder().externalId(externalId).build();
	
	    when(orderRepository.findByExternalId(externalId.toString())).thenReturn(Optional.of(new Order()));
	
	    Exception exception = assertThrows(IllegalArgumentException.class, () -> orderService.receiveOrder(orderDTO));
	
	    assertEquals("Pedido duplicado: " + externalId, exception.getMessage());
	}
	
	@Test
	void testReceiveOrder_Success() throws Exception {
	    UUID externalId = UUID.randomUUID();
	    OrderDTO orderDTO = OrderDTO.builder().externalId(externalId).build();
	
	    when(orderRepository.findByExternalId(externalId.toString())).thenReturn(Optional.empty());
	    doNothing().when(orderProducer).sendMessage(anyString());
	    when(objectMapper.writeValueAsString(orderDTO)).thenReturn("serializedOrder");
	
	    OrderDTO result = orderService.receiveOrder(orderDTO);
	
	    verify(orderProducer, times(1)).sendMessage("serializedOrder");
	    assertEquals(orderDTO, result);
	}
	
	@Test
	void testCreateOrder_Success() {
	    ProductDTO productDTO = ProductDTO.builder()
	            .name("Product 1")
	            .price(BigDecimal.TEN)
	            .quantity(2)
	            .build();
	
	    OrderDTO orderDTO = OrderDTO.builder()
	            .externalId(UUID.randomUUID())
	            .products(List.of(productDTO))
	            .build();
	
	    Order order = orderService.orderDtoToOrder(orderDTO);
	
	    when(orderRepository.save(any(Order.class))).thenReturn(order);
	
	    orderService.createOrder(orderDTO);
	
	    verify(orderRepository, times(1)).save(any(Order.class));
	}
	
	@Test
	void testProcessOrder_OrderNotFound_ShouldThrowException() {
	    when(orderRepository.findById(1L)).thenReturn(Optional.empty());
	
	    Exception exception = assertThrows(IllegalArgumentException.class, () -> orderService.processOrder(1L));
	
	    assertEquals("Pedido não encontrado: 1", exception.getMessage());
	}
	
	@Test
	void testProcessOrder_Success() {
	    Order order = Order.builder().id(1L).status(OrderStatus.RECEIVED).build();
	    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
	
	    orderService.processOrder(1L);
	
	    verify(orderRepository, times(1)).save(order);
	    assertEquals(OrderStatus.PROCESSED, order.getStatus());
	}
	
	@Test
	void testGetOrders_Success() {
	    Order order = Order.builder()
	            .id(1L)
	            .externalId(UUID.randomUUID().toString())
	            .products(List.of(Product.builder().name("Product 1").price(BigDecimal.TEN).quantity(1).build()))
	            .totalPrice(BigDecimal.TEN)
	            .status(OrderStatus.RECEIVED)
	            .build();
	
	    Page<Order> orderPage = new PageImpl<>(List.of(order));
	    when(orderRepository.findAll(PageRequest.of(0, 10))).thenReturn(orderPage);
	
	    Page<OrderDTO> result = orderService.getOrders(PageRequest.of(0, 10));
	
	    assertEquals(1, result.getTotalElements());
	    assertEquals(order.getId(), result.getContent().get(0).getId());
	}
	
	@Test
	void testOrderDtoToOrder() {
	    ProductDTO productDTO = ProductDTO.builder()
	            .name("Product 1")
	            .price(BigDecimal.TEN)
	            .quantity(2)
	            .build();
	
	    OrderDTO orderDTO = OrderDTO.builder()
	            .externalId(UUID.randomUUID())
	            .products(List.of(productDTO))
	            .build();
	
	    Order order = orderService.orderDtoToOrder(orderDTO);
	
	    assertNotNull(order);
	    assertEquals(orderDTO.getExternalId().toString(), order.getExternalId());
	    assertEquals(orderDTO.getProducts().size(), order.getProducts().size());
	}
	
	@Test
	void testToOrderDTO() {
	    Product product = Product.builder()
	            .name("Product 1")
	            .price(BigDecimal.TEN)
	            .quantity(2)
	            .build();
	
	    Order order = Order.builder()
	            .id(1L)
	            .externalId(UUID.randomUUID().toString())
	            .products(List.of(product))
	            .totalPrice(BigDecimal.valueOf(20))
	            .build();
	
	    OrderDTO result = orderService.toOrderDTO(order);
	
	    assertNotNull(result);
	    assertEquals(order.getId(), result.getId());
	    assertEquals(order.getProducts().size(), result.getProducts().size());
	    assertEquals(order.getTotalPrice(), result.getTotalPrice());
	}
}
