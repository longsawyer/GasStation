package gasstation;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.Table;
import org.springframework.beans.BeanUtils;

import gasstation.event.CanceledSold;
import gasstation.event.Sold;

/**
 * 판매내역
 * @author Administrator
 *
 */
@Entity
@Table(name="T_SALE")
public class Sale {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long saleId;
    private String productId;
    private String productName;
    private Long price;
    private Double qty;
    private Double amount;
    private Long accountId;
    private String accountName;

    @PostPersist
    public void onPostPersist(){
        Sold sold = new Sold();
        BeanUtils.copyProperties(this, sold);
        sold.publishAfterCommit();
    }
    
    /**
     * 삭제시, 보상처리
     */
    @PostRemove
    public void onPostRemove(){
    	CanceledSold canceledSold =new CanceledSold();
        BeanUtils.copyProperties(this, canceledSold);
        canceledSold.publishAfterCommit();
    }
    
    public Long getSaleId() {
		return saleId;
	}

	public void setSaleId(Long saleId) {
		this.saleId = saleId;
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
    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }
    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }




}
