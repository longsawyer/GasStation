package gasstation;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 판매집계
 * @author Administrator
 *
 */
@Entity
@Table(name = "V_SALES_S")
public class SalesSummary {

	@Id
	private String 	productId;		// 유종
	private String 	productName;	// 유종명
	private Double 	qty;			// 총수량
	private Double 	amount;			// 총금액
	
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
	
	public Double getAmount() {
		return amount;
	}
	
	public void setAmount(Double amount) {
		this.amount = amount;
	}
	
	public void addAmount(Double amount) {
		this.amount += amount;
	}
}
