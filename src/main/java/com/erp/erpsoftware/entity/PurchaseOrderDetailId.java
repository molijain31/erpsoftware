package com.erp.erpsoftware.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderDetailId implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Integer slNo;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PurchaseOrderDetailId that = (PurchaseOrderDetailId) o;
        return Objects.equals(orderId, that.orderId) && Objects.equals(slNo, that.slNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, slNo);
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Integer getSlNo() { return slNo; }
    public void setSlNo(Integer slNo) { this.slNo = slNo; }
}
