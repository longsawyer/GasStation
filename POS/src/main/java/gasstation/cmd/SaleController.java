package gasstation.cmd;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.NumberUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gasstation.PosApplication;
import gasstation.ProductMenu;
import gasstation.Sale;
import gasstation.external.Account;
import gasstation.external.StationService;
import gasstation.external.StockFlow;
import gasstation.repo.ProductMenuRepository;
import gasstation.repo.SaleRepository;

@RestController
public class SaleController {
	private Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired SaleRepository saleRepository;
	@Autowired ProductMenuRepository productMenuRepository;

	/**
	 * 상품판매
	 * @param productId
	 * @param price
	 * @param qty
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/sales", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	public boolean sales(
			@RequestParam("productId") String productId, 
			@RequestParam("qty") Double qty
				) throws Exception {

		// init
		boolean retValue = false;
		logger.info("### 판매");
				
		Optional<ProductMenu> opt =productMenuRepository.findById(productId);
		// 상품이 존재하면
		if( opt.isPresent()) {
			ProductMenu productMenu =opt.get();
			
			Sale sale = new Sale();
			sale.setAccountId(1L); // 계정은 외상계정을 말하나...현금계정도 일단 외상으로 취급한다
			sale.setProductId(productId);
			sale.setProductName(productMenu.getProductName());
			sale.setPrice(productMenu.getPrice());
			sale.setQty(qty);
			sale.setAmount( qty*productMenu.getPrice());
			saleRepository.save(sale);
			
			// 동기처리
			StationService stationService =PosApplication.applicationContext.getBean(StationService.class);
			
			// 재고감소
			StockFlow stockFlow =new StockFlow();
			BeanUtils.copyProperties(sale, stockFlow);
			stockFlow.setQty( -1 *stockFlow.getQty());	// 재고는 감소처리해야 한다.
			stationService.outcome(stockFlow);
			
			// TODO 계정(현금) 증감 => 나중에 구현필요

			retValue = true;
		} else {
			logger.error("### 상품등록 필요 =" +productId);
		}
		
		return retValue;
	}
	
	/**
	 * 판매취소
	 * 		보상거래이다. 보정거래라고 부르기도 한다
	 * @param saleId
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/cancelSales", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	public boolean cancelSales(
			@RequestParam("saleId") Long saleId
				) throws Exception {

		// init
		boolean retValue = false;
		logger.info("### 판매취소");
		
		Optional<Sale> opt =saleRepository.findById(saleId);
		
		if( opt.isPresent()) {
			saleRepository.deleteById(saleId);
			retValue = true;
		} else {
			logger.error("### 취소할 내역 없음");
		}
		
		return retValue;
	}
}
