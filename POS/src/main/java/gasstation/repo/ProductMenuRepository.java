package gasstation.repo;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import gasstation.ProductMenu;

@RepositoryRestResource(collectionResourceRel="productMenus", path="productMenus")
public interface ProductMenuRepository extends PagingAndSortingRepository<ProductMenu, String>{


}
