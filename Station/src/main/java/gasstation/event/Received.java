
package gasstation.event;

import gasstation.AbstractEvent;

/**
 * 주문됨
 * @author Administrator
 *
 */
public class Received extends AbstractEvent {

    private Long 	stockId;
    private Long 	orderId;	// 주문으로 수령된것이라면...
    private String 	productId;
    private String 	productName;
    private Double 	qty;
    private Double 	qtyToBe;
    private String 	reason;
    
    public Long getStockId() {
		return stockId;
	} 

	public void setStockId(Long stockId) {
		this.stockId = stockId;
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
    
    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }
    public Double getQtyToBe() {
        return qtyToBe;
    }

    public void setQtyToBe(Double qtyToBe) {
        this.qtyToBe = qtyToBe;
    }
    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

	public Long getOrderId() {
		return orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}
}

