package gasstation.repo;

import java.util.Optional;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import gasstation.StockFlow;

@RepositoryRestResource(collectionResourceRel="stockFlows", path="stockFlows")
public interface StockFlowRepository extends PagingAndSortingRepository<StockFlow, Long>{
	Optional<StockFlow> findByOrderId( @Param("order_id") Long orderId);

}
