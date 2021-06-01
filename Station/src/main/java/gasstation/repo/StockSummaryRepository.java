package gasstation.repo;

import org.springframework.data.repository.CrudRepository;
import gasstation.StockSummary;

public interface StockSummaryRepository extends CrudRepository<StockSummary, String> {


}