package gasstation.cmd;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gasstation.Product;
import gasstation.event.ProductChanged;
import gasstation.repo.ProductRepository;

@RestController
public class ProductController {
	private Logger loggger = LoggerFactory.getLogger(getClass());
	@Autowired ProductRepository productRepository;

	/**
	 * 상품정보 재전송
	 * 
	 * @param productId
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/products/sync", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	public boolean placeOrder(
			@RequestParam("productId") String productId,
			@RequestParam( value="price", required = false) Long price
			) throws Exception {
		boolean retValue =false;
		
		Optional<Product> opt = productRepository.findByProductId(productId);
		if (opt.isPresent()) {
			loggger.info("### 상품정보 재전송");
			
			Product product = opt.get();
			// 가격변경이 있다면 반영
			if( price !=null && price !=product.getPrice()) {
				product.setPrice(price);
				productRepository.save(product);	// 저장하면 자동반영
			} 
			ProductChanged productChanged = new ProductChanged();
			BeanUtils.copyProperties(product, productChanged);
			productChanged.setResend(true);	// 재전송
			productChanged.publish();
			
			retValue =true;
		} else {
			loggger.error("### 상품정보 못찾음");
		}

		return retValue;
	}
}
