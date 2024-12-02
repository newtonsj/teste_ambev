package br.com.ambev.order_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.ambev.order_service.model.Order;
import br.com.ambev.order_service.model.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long>{

	Optional<Order> findByExternalId(String string);
	
	@Query("SELECT o FROM Order o WHERE o.status = 'RECEIVED'")
	List<Order> findAllPendingOrders();
	
	@Modifying
	@Query(value = "UPDATE orders SET status = 'PROCESSING' WHERE id IN " +
	               "(SELECT id FROM orders WHERE status = 'RECEIVED' LIMIT :batchSize) " +
	               "RETURNING *", nativeQuery = true)
	List<Order> findAndMarkOrdersAsInProgress(@Param("batchSize") int batchSize);
	
	@Modifying
	@Query("UPDATE Order o SET o.status = :status WHERE o.id = :orderId")
	void updateStatus(@Param("orderId") Long orderId, @Param("status") OrderStatus status);
}
