
package gasstation.event;

import gasstation.AbstractEvent;

/**
 * 상품변경됨 Event
 * @author Administrator
 *
 */
public class ProductChanged extends AbstractEvent {
	private String productId;
    private String productName;
    private Long price;
    private boolean isResend;	// 재전송여부
    
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

	public boolean isResend() {
		return isResend;
	}

	public void setResend(boolean isResend) {
		this.isResend = isResend;
	}
}

