package br.com.ambev.order_service.client;

import java.util.List;

import br.com.ambev.order_service.dto.OrderDTO;
import br.com.ambev.order_service.model.OrderStatus;
import br.com.ambev.order_service.repository.OrderRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ExternalApiClientFallback implements ExternalApiClient{

    private final OrderRepository orderRepository;
	@Override
    public void sendOrders(List<OrderDTO> orders) {
		
		orders.forEach(order -> {
				orderRepository.updateStatus(order.getId(), OrderStatus.RECEIVED);
			});
    }
}
