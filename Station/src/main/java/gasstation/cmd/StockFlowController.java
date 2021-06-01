package gasstation.cmd;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gasstation.StockFlow;
import gasstation.StockSummary;
import gasstation.repo.StockFlowRepository;
import gasstation.repo.StockSummaryRepository;

/**
 * 재고흐름처리
 * @author Administrator
 *
 */
@RestController
public class StockFlowController {
	private Logger logger =LoggerFactory.getLogger(getClass());
	
	@Autowired StockFlowRepository 		stockFlowRepository;
	@Autowired StockSummaryRepository 	stockSummaryRepository;
	
	/**
	 * 재고흐름 일반
	 * @param stockFlow
	 * @return
	 */
	@RequestMapping(path="/stockFlows", method= RequestMethod.POST, produces = "application/json;charset=UTF-8")
	public boolean outcome(@RequestBody StockFlow stockFlow) {
		// init
		boolean retValue = false;
		
		logger.info("### 재고증감");
		// 재고증감
		Optional<StockSummary> optStock =stockSummaryRepository.findById(stockFlow.getProductId());
		if( optStock.isPresent()) {
			StockSummary stockSummary =optStock.get();
			stockSummary.addQty(stockFlow.getQty());
			stockSummaryRepository.save(stockSummary);
			logger.info("재고 " + stockSummary.getProductName() +"=" +stockSummary.getQty());
		} else {
			StockSummary stockSummary =new StockSummary();
			BeanUtils.copyProperties(stockFlow, stockSummary);
			stockSummaryRepository.save(stockSummary);
			logger.info("재고 " + stockSummary.getProductName() +"=" +stockSummary.getQty());
		}
		
		return retValue;
	}
	
	/**
	 * 입고확정
	 * @param id
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/stocks/confirmStock", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	public boolean confirmStock( @RequestParam("orderId") Long orderId) throws Exception {
		// init
		boolean retValue = false;
		
		logger.info("### 입고확정");
		Optional<StockFlow> opt =stockFlowRepository.findByOrderId(orderId);
		if( opt.isPresent()) {
			StockFlow stockFlow =opt.get();
			// 입고물량 확정
			stockFlow.setQty( stockFlow.getQtyToBe());
			stockFlowRepository.save(stockFlow);
			
			// 재고증감
			Optional<StockSummary> optStock =stockSummaryRepository.findById(stockFlow.getProductId());
			if( optStock.isPresent()) {
				StockSummary stockSummary =optStock.get();
				stockSummary.addQty(stockFlow.getQty());
				stockSummaryRepository.save(stockSummary);
				logger.info("재고 " + stockSummary.getProductName() +"=" +stockSummary.getQty());
			} else {
				StockSummary stockSummary =new StockSummary();
				BeanUtils.copyProperties(stockFlow, stockSummary);
				stockSummaryRepository.save(stockSummary);
				logger.info("재고 " + stockSummary.getProductName() +"=" +stockSummary.getQty());
			}
			
			retValue = true;
			logger.info("### 입고확정 => 확정됨");
		} else {
			logger.error("### 입고확정할 주문이 없음 orderId=" +orderId);
		}
		
		return retValue;
	}

}
