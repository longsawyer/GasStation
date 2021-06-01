package gasstation.repo;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import gasstation.ProductMaster;

@RepositoryRestResource(collectionResourceRel="productMasters", path="productMasters")
public interface ProductMasterRepository extends PagingAndSortingRepository<ProductMaster, String>{


}
