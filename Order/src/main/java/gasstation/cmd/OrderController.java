package gasstation.cmd;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gasstation.Order;
import gasstation.Product;
import gasstation.repo.OrderRepository;
import gasstation.repo.ProductRepository;

@RestController
public class OrderController {
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired OrderRepository 	orderRepository;
	@Autowired ProductRepository productRepository;
	
	@PostConstruct
	public void init( ) {
		
	}

	@RequestMapping(value = "/orders/placeOrder", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	public boolean placeOrder(
			@RequestParam("productId") String productId, 
			@RequestParam("qty") Double qty,
			@RequestParam("destAddr") String destAddr	) throws Exception {

		// init
		boolean retValue = false;
		
		logger.info("### 주문");
		
		// DB에서 상품검색...실제는 DB말고 캐시를 써야함
		Optional<Product> opt =productRepository.findByProductId(productId);
		if( opt.isPresent()) {
			// 상품정보
			Product product =opt.get();
			
			// 주문생성
			Order order = new Order();
			order.setProductId(productId);
			order.setProductName( product.getProductName());
			order.setQty(qty);
			order.setDestAddr(destAddr);
			order.setOrderDate(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
			orderRepository.save(order);
			
			retValue = true;
		} else {
			logger.error("### 상품존재하지 않음=" + productId);
		}

		return retValue;
	}

	/**
	 * 점포명 출력
	 * @return
	 */
	@RequestMapping(value = "/orders/station", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	public String station() {
		logger.info("### 점포=" + System.getenv().get("station_nm") + ", " + System.getenv().get("station_cd"));
		return System.getenv().get("station_nm") + ", " + System.getenv().get("station_cd");
	}
}
