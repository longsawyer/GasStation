package gasstation;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 상품메뉴
 * @author Administrator
 *
 */
@Entity
@Table(name="T_MENU_M")
public class ProductMenu {

    @Id
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
