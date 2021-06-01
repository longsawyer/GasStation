package gasstation;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.Table;

import org.springframework.beans.BeanUtils;

import gasstation.event.ProductChanged;

@Entity
@Table(name="T_PRODUCT_M")
//@Document(collection = "T_PRODUCT_M")
public class Product {

    @Id
    //private String id;
    //@GeneratedValue(strategy=GenerationType.AUTO)
    private String 	productId;		// 상품ID
    private String 	productName;	// 상품명
    private Long 	price;			// 가격

    /**
     * 상품정보 생성
     */
    @PostPersist
    public void onPostPersist(){
        ProductChanged productChanged = new ProductChanged();
        BeanUtils.copyProperties(this, productChanged);
        productChanged.publishAfterCommit();
    }
    
    /**
     * 상품정보 변경
     */
    @PostUpdate
    public void onPostUpdate() {
    	 ProductChanged productChanged = new ProductChanged();
         BeanUtils.copyProperties(this, productChanged);
         productChanged.publishAfterCommit();
    }
    
    /**
     * 강제전송용
     */
    public void fireEvent() {
    	 ProductChanged productChanged = new ProductChanged();
         BeanUtils.copyProperties(this, productChanged);
         productChanged.publish();
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }
    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

//	public String getId() {
//		return id;
//	}
//
//	public void setId(String id) {
//		this.id = id;
//	}

}
