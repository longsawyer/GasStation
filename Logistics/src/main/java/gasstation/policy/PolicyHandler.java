package gasstation.policy;

import java.util.logging.Logger;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import gasstation.Shipment;
import gasstation.config.kafka.KafkaProcessor;
import gasstation.event.Ordered;
import gasstation.repo.ShipmentRepository;

@Service
public class PolicyHandler{
	private Logger logger =Logger.getGlobal();
    @Autowired ShipmentRepository shipmentRepository;

    /**
     * 주문이 발생했을때, 자동으로 배송발생
     * @param ordered
     */
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverOrdered_RequestOrder(@Payload Ordered ordered){

        if(!ordered.validate()) return;

        logger.info("\n\n##### listener RequestOrder : " + ordered.toJson() + "\n\n");

        Shipment shipment = new Shipment();
        BeanUtils.copyProperties(ordered, shipment);
        // 차량번호는 임의생성
        shipment.setCarNumber("CAR#" + Math.round( Math.random()*1000));
        shipmentRepository.save(shipment);
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}

}
