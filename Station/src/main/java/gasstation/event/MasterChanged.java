
package gasstation.event;

import gasstation.AbstractEvent;

/**
 * 주유소 상품마스터 변경시 Event => POS
 * @author Administrator
 *
 */
public class MasterChanged extends AbstractEvent {
    private String productId;
    private String productName;
    private Long price;
    
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

