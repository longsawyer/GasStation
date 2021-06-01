package gasstation.policy;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import gasstation.OrderStatus;
import gasstation.config.kafka.KafkaProcessor;
import gasstation.event.Ordered;
import gasstation.event.Received;
import gasstation.event.Shipped;
import gasstation.repo.OrderStatusRepository;


@Service
public class OrderStatusViewHandler {
	private Logger logger = LoggerFactory.getLogger(getClass());
	
    @Autowired
    private OrderStatusRepository orderStatusRepository;
    
    /**
     * 주문시, 주문상태 만듬
     * @param shipped
     */
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverOrdered_NotifyOrderStatus(@Payload Ordered ordered){
        if(!ordered.validate()) return;

        logger.info("\n\n##### listener NotifyOrderStatus : " + ordered.toJson() + "\n\n");
        
        Optional<OrderStatus> opt =orderStatusRepository.findById(ordered.getOrderId());
        if( opt.isPresent()) {
        	OrderStatus orderStatus =opt.get();
        	orderStatus.setStatus(ordered.getEventType());
        	orderStatusRepository.save(orderStatus);
        } else {
        	OrderStatus orderStatus =new OrderStatus();
        	BeanUtils.copyProperties(ordered, orderStatus);
        	orderStatus.setStatus(ordered.getEventType());
        	orderStatusRepository.save(orderStatus);
        }
    }
    
    /**
     * 배송시, 주문상태 배송으로 변경
     * @param shipped
     */
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverShipped_NotifyOrderStatus(@Payload Shipped shipped){
        if(!shipped.validate()) return;

        logger.info("\n\n##### listener NotifyOrderStatus : " + shipped.toJson() + "\n\n");
        
        Optional<OrderStatus> opt =orderStatusRepository.findById(shipped.getOrderId());
        if( opt.isPresent()) {
        	OrderStatus orderStatus =opt.get();
        	orderStatus.setStatus(shipped.getEventType());
        	orderStatusRepository.save(orderStatus);
        } else {
        	logger.error("주문내역 못찾음 orderId=" + shipped.getOrderId());	
        }
    }
    
    /**
     * 수령시, 주문상태 수령으로 변경
     * @param received
     */
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverReceived_NotifyOrderStatus(@Payload Received received){
    	// 유효성체크
        if(!received.validate()) {
        	return;
        }
        if( received.getOrderId() ==null) {
        	return;
        }

        logger.info("\n\n##### listener NotifyOrderStatus : " + received.toJson() + "\n\n");
        
        Optional<OrderStatus> opt =orderStatusRepository.findById(received.getOrderId());
        if( opt.isPresent()) {
        	OrderStatus orderStatus =opt.get();
        	
        	// 입고량에 따라서 확정여부가 판단됨
        	if( received.getQty() > 0) {
        		// 입고확정
        		orderStatus.setStatus("confirmed");
        	} else {
        		// 수령예정
        		orderStatus.setStatus(received.getEventType());
        	}
        	
        	orderStatusRepository.save(orderStatus);
        } else {
        	logger.error("주문내역 못찾음 orderId=" + received.getOrderId());	
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}
}