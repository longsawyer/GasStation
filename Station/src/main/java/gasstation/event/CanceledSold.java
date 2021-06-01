
package gasstation.event;

import gasstation.AbstractEvent;

/**
 * 주문취소 Event
 * @author Administrator
 *
 */
public class CanceledSold extends AbstractEvent {
    private String productId;
    private String productName;
    private Long price;
    private Double qty;
    private Double amount;
    private Long accountId;
    private String accountName;

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

