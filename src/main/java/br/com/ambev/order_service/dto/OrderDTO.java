package br.com.ambev.order_service.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class OrderDTO {

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Long id;
	
    private UUID externalId;
    private List<ProductDTO> products;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal totalPrice;

}
