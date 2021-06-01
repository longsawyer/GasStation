package gasstation.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import gasstation.ProductMaster;
import gasstation.StockFlow;
import gasstation.config.kafka.KafkaProcessor;
import gasstation.event.CanceledSold;
import gasstation.event.MasterChanged;
import gasstation.event.ProductChanged;
import gasstation.event.Shipped;
import gasstation.repo.AccountRepository;
import gasstation.repo.ProductMasterRepository;
import gasstation.repo.StockFlowRepository;

@Service
public class PolicyHandler{
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired StockFlowRepository stockFlowRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired ProductMasterRepository productMasterRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverShipped_ReserveIncome(@Payload Shipped shipped){

        if(!shipped.validate()) return;

        logger.info("\n\n##### listener ReserveIncome : " + shipped.toJson() + "\n\n");

        // 재고흐름 추가
        StockFlow stockFlow = new StockFlow();
        BeanUtils.copyProperties(shipped, stockFlow);
        
        // 아직 입고확정을 하지 않는다
        // 입고확정은 사용자에게 받는다
        stockFlow.setQtyToBe( stockFlow.getQty());
        stockFlow.setQty( 0.0);
        stockFlowRepository.save(stockFlow);
    } 

    /**
     * 주문의 상품마스터가 변경되면
     * @param productChanged
     */
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverProductChanged_ChangeMaster(@Payload ProductChanged productChanged){

        if(!productChanged.validate()) return;

        logger.info("\n\n##### listener ChangeMaster : " + productChanged.toJson() + "\n\n");

        // 상품마스터 변경
        ProductMaster productMaster = new ProductMaster();
        BeanUtils.copyProperties(productChanged, productMaster);
        productMasterRepository.save(productMaster);
        
        // 단순 재전송건이면, POS로 재전송
        if( productChanged.isResend() ==true) {
        	MasterChanged masterChanged = new MasterChanged();
            BeanUtils.copyProperties(productChanged, masterChanged);
            masterChanged.publish();
        }
    }
    
    /**
     * 주문이 취소되면, 재고를 다시 증가시킨다
     * @param canceledSold
     */
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCanceledSold_cancelStock(@Payload CanceledSold canceledSold){

        if(!canceledSold.validate()) return;

        logger.info("\n\n##### listener CanceledSold : " + canceledSold.toJson() + "\n\n");

        // 재고흐름 추가
        StockFlow stockFlow = new StockFlow();
        BeanUtils.copyProperties(canceledSold, stockFlow);
        
        // 입고처리 ( 판매는 마이너스처리이므로...)
        stockFlowRepository.save(stockFlow);
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
