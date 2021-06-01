package gasstation.repo;

import org.springframework.data.repository.CrudRepository;
import gasstation.OrderStatus;

public interface OrderStatusRepository extends CrudRepository<OrderStatus, Long> {


}