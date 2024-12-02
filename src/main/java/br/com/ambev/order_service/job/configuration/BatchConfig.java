package br.com.ambev.order_service.job.configuration;

import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import br.com.ambev.order_service.client.ExternalApiClient;
import br.com.ambev.order_service.dto.OrderDTO;
import br.com.ambev.order_service.model.Order;
import br.com.ambev.order_service.model.OrderStatus;
import br.com.ambev.order_service.repository.OrderRepository;
import br.com.ambev.order_service.service.OrderService;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class BatchConfig {

    private final OrderRepository orderRepository; 
    private final ExternalApiClient externalApiClient; 	 
	private final OrderService orderService;

    public BatchConfig(OrderRepository orderRepository, ExternalApiClient externalApiClient, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.externalApiClient = externalApiClient;
		this.orderService = orderService;
    }
    
    @Bean
    Job job(JobRepository jobRepository, Step processOrdersStep) {
        return new JobBuilder("processOrdersJob", jobRepository)
                .start(processOrdersStep)
                .build();
    }


    @Bean
    Step processOrdersStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("processOrdersStep", jobRepository)
                .<Order, OrderDTO>chunk(100, transactionManager)
                .reader(orderReader())
                .processor(orderProcessor())
                .writer(orderWriter())
                .build();
    }
    

	    @Bean
	    @StepScope
	    ItemReader<Order> orderReader() {
	    	  return new ItemReader<>() {
	              private List<Order> orders = null;
	              private int index = 0;

	              @Override
	              public Order read() {
	                  if (orders == null) {
	                	  
	                	  log.info("Iniciando execução do job de processamento de pedidos");
	                	  
	                      orders = orderRepository.findAndMarkOrdersAsInProgress(100);
	                      
	                      index = 0; 
	                      if (orders.isEmpty()) {
	                          log.info("Nenhum pedido pendente encontrado.");
	                          return null; 
	                      }

	                     
	                      log.info("Pedidos carregados: " + orders.size());
	                      
	                  }
	                  if (index < orders.size()) {
	                      return orders.get(index++);
	                  }
	                  return null;
	              }
	          };
	    }

	    @Bean
	    ItemProcessor<Order, OrderDTO> orderProcessor() {
	        return order -> {

	        	log.info("Processando pedido: " + order.getExternalId());
	            return orderService.toOrderDTO(order);
	        };
	    }

	    @Bean
	    ItemWriter<OrderDTO> orderWriter() {
	        return items -> {	           
	            log.info("Enviando pedidos para API externa.");
	            List<OrderDTO> ordersList = (List<OrderDTO>) items.getItems();
	            externalApiClient.sendOrders(ordersList);   
	            ordersList.forEach(orderDTO -> {
					orderRepository.updateStatus(orderDTO.getId(), OrderStatus.PROCESSED);
				});
	            orderRepository.updateStatus(null, null);
	        };
	    }
}
