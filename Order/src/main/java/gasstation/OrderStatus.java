package gasstation;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 주문상태
 * @author Administrator
 *
 */
@Entity
@Table(name="V_ORDER_STATUS")
public class OrderStatus {
        @Id
        private Long 	orderId;
        private String 	productId;		// 유종
        private String 	productName;	// 유종명
        private Double 	qty;			// 수량			
        private String 	destAddr;		// 배송지
        private String 	orderDate;		// 주문일자
        private String 	status;			// 주문상태

		public Long getOrderId() {
			return orderId;
		}

		public void setOrderId(Long orderId) {
			this.orderId = orderId;
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

		public String getDestAddr() {
			return destAddr;
		}

		public void setDestAddr(String destAddr) {
			this.destAddr = destAddr;
		}

		public String getOrderDate() {
			return orderDate;
		}

		public void setOrderDate(String orderDate) {
			this.orderDate = orderDate;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

}
