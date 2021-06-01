package gasstation.repo;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import gasstation.Shipment;

@RepositoryRestResource(collectionResourceRel="shipments", path="shipments")
public interface ShipmentRepository extends PagingAndSortingRepository<Shipment, Long>{


}
