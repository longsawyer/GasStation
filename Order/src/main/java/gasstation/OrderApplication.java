package gasstation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.ApplicationContext;

import gasstation.config.kafka.KafkaProcessor;
import gasstation.repo.ProductRepository;


@SpringBootApplication
@EnableBinding(KafkaProcessor.class)
@EnableFeignClients
public class OrderApplication {
    protected static ApplicationContext applicationContext;
    
    public static void main(String[] args) {
        applicationContext = SpringApplication.run(OrderApplication.class, args);
        
        // 상품등록
        Logger logger = LoggerFactory.getLogger(OrderApplication.class);
        
        // DAO
        ProductRepository productRepository =applicationContext.getBean(ProductRepository.class);
        
        logger.info("### 상품초기화");
		// H2가 메모리 DB 때문에...임의상품 유종을 몇개 넣어둔다.
		Product product =null;
		
		// 혹시 있다면 모두삭제...(MongoDB일 경우)
		productRepository.deleteAll();
		
		// 휘발유
		product =new Product();
		product.setProductId("CD1001");
		product.setProductName("GAS");
		product.setPrice(2000L);
		productRepository.save(product);
		product.fireEvent();
		
		// 경유
		product =new Product();
		product.setProductId("CD1004");
		product.setProductName("Diesel");
		product.setPrice(1500L);
		productRepository.save(product);
		product.fireEvent();
    }
}
