package gasstation.repo;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import gasstation.Account;

@RepositoryRestResource(collectionResourceRel="accounts", path="accounts")
public interface AccountRepository extends PagingAndSortingRepository<Account, Long>{


}
