package gasstation.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import gasstation.ProductMenu;
import gasstation.config.kafka.KafkaProcessor;
import gasstation.event.MasterChanged;
import gasstation.repo.ProductMenuRepository;
import gasstation.repo.SaleRepository;

@Service
public class PolicyHandler{
	private Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired SaleRepository saleRepository;
    @Autowired ProductMenuRepository productMenuRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverMasterChanged_ChangeMenu(@Payload MasterChanged masterChanged){

        if(!masterChanged.validate()) return;
        if( StringUtils.isEmpty(masterChanged.getProductId())) return;

        logger.info("\n\n##### listener ChangeMenu : " + masterChanged.toJson() + "\n\n");

        // 상품정보 변경
        ProductMenu productMenu = new ProductMenu();
        BeanUtils.copyProperties(masterChanged, productMenu);
        productMenuRepository.save(productMenu);
        
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
