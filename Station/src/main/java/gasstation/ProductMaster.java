package gasstation;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.Table;

import org.springframework.beans.BeanUtils;

import gasstation.event.MasterChanged;

/**
 * 상품마스터
 * @author Administrator
 *
 */
@Entity
@Table(name="T_PRODUCT_M")
public class ProductMaster {

    @Id
    private String productId;
    private String productName;
    private Long price;

    @PostPersist
    public void onPostPersist(){
        MasterChanged masterChanged = new MasterChanged();
        BeanUtils.copyProperties(this, masterChanged);
        masterChanged.publishAfterCommit();
    }
    
    @PostUpdate
    public void onPostUpdate() {
        MasterChanged masterChanged = new MasterChanged();
        BeanUtils.copyProperties(this, masterChanged);
        masterChanged.publishAfterCommit();
    }
    
    public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
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




}
