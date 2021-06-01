package gasstation.policy;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import gasstation.SalesSummary;
import gasstation.config.kafka.KafkaProcessor;
import gasstation.event.CanceledSold;
import gasstation.event.Sold;
import gasstation.repo.SalesSummaryRepository;

/**
 * 판매뷰 핸들러
 * @author Administrator
 *
 */
@Service
public class SalesSummaryViewHandler {
	private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private SalesSummaryRepository salesSummaryRepository;
    
    /**
     * 판매됬을때 판매집계를 한다
     * @param sold 
     */
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverSold_ChangeSalesSummary(@Payload Sold sold){

        if(!sold.validate()) return;

        logger.info("\n\n##### listener ChangeSalesSummary : " + sold.toJson() + "\n\n");
        
        Optional<SalesSummary> opt =salesSummaryRepository.findById(sold.getProductId());
        if( opt.isPresent()) {
        	SalesSummary salesSummary =opt.get();
        	salesSummary.addQty(	sold.getQty());
        	salesSummary.addAmount(	sold.getAmount());
        	salesSummaryRepository.save(salesSummary);
        } else {
        	SalesSummary salesSummary =new SalesSummary();
        	BeanUtils.copyProperties(sold, salesSummary);
        	salesSummaryRepository.save(salesSummary);
        }
    }
    
    /**
     * 주문이 취소되면, 판매감소한다
     * @param canceledSold
     */
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCanceledSold_cancelStock(@Payload CanceledSold canceledSold){

    	if(!canceledSold.validate()) return;

        logger.info("\n\n##### listener ChangeSalesSummary : " + canceledSold.toJson() + "\n\n");
        
        Optional<SalesSummary> opt =salesSummaryRepository.findById(canceledSold.getProductId());
        if( opt.isPresent()) {
        	SalesSummary salesSummary =opt.get();
        	salesSummary.addQty(	-1* canceledSold.getQty());
        	salesSummary.addAmount(	-1* canceledSold.getAmount());
        	salesSummaryRepository.save(salesSummary);
        } else {
        	logger.error("감소시길 재고집계내역이 없음");
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}