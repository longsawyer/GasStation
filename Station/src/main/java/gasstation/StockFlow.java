package gasstation;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.Table;

import org.springframework.beans.BeanUtils;

import gasstation.event.Received;

/**
 * 재고흐름
 * @author Administrator
 *
 */
@Entity
@Table(name="T_STOCK_D")
public class StockFlow {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
    private Long stockId;
    private String productId;
    private String productName;
    private Double qty;
    private Double qtyToBe;
    private String reason;
    private Long orderId;

    @PostPersist
    public void onPostPersist(){
        Received received = new Received();
        BeanUtils.copyProperties(this, received);
        received.publishAfterCommit();
    } 
    
    @PostUpdate
    public void onPostUpdate(){
        Received received = new Received();
        BeanUtils.copyProperties(this, received);
        received.publishAfterCommit();
    }
    
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
