package gasstation;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 재고
 * 		재고는 유량별로 집계한다. 
 * 		탱크별 집계는 고려하지 않는다.
 * @author Administrator
 *
 */
@Entity
@Table(name="V_STOCK_S")
public class StockSummary {
    @Id
    private String 	productId;		// 유종
    private String 	productName;	// 유종명
    private Double 	qty;			// 재고수량
    
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
	
	public void addQty(Double qty) {
		this.qty += qty;
	}
}
