package gasstation.repo;

import java.util.Optional;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import gasstation.Product;

@RepositoryRestResource(collectionResourceRel="product", path="product")
public interface ProductRepository extends PagingAndSortingRepository<Product, Long>{
	Optional<Product> findByProductId( @Param("product_id") String productId);
}

//public interface ProductRepository extends MongoRepository<Product, String>{
//	Optional<Product> findByProductId( @Param("product_id") String productId);
//}