package gasstation.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import gasstation.StockFlow;
import gasstation.cmd.StockFlowController;
import gasstation.config.kafka.KafkaProcessor;
import gasstation.event.CanceledSold;
import gasstation.repo.StockSummaryRepository;

@Service
public class StockSummaryViewHandler {
	private Logger logger =LoggerFactory.getLogger(getClass());
	
    @Autowired
    private StockSummaryRepository stockSummaryRepository;
    
    @Autowired
    private StockFlowController stockFlowController;
    
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
        
        // DAO를 쓰지 않고, 재고관련 프로세스가 있는 cmd controll을 쓴다
        stockFlowController.outcome(stockFlow);
    }
    
    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}
}