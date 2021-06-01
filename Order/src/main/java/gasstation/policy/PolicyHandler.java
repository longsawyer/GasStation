package gasstation.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import gasstation.config.kafka.KafkaProcessor;
import gasstation.repo.OrderRepository;
import gasstation.repo.ProductRepository;

@Service
public class PolicyHandler{
	private Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired OrderRepository orderRepository;
    @Autowired ProductRepository productRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
