package com.utetra.entity;

import com.utetra.common.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ban ghi thanh toan cua mot don hang.
 *
 * Order da co san paymentMethod va paymentStatus de hien thi nhanh;
 * bang nay luu chi tiet giao dich voi cong thanh toan (VNPAY / MOMO):
 * ma giao dich, so tien that su tru, noi dung cong tra ve.
 * Mot don co the co nhieu ban ghi neu thanh toan hong roi tra lai.
 */
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method = PaymentMethod.COD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    /** Ma giao dich ben cong thanh toan tra ve (vnp_TransactionNo, momo transId...). */
    @Column(length = 100)
    private String transactionId;

    /** Noi dung cong thanh toan tra ve, giu lai de doi soat khi co su co. */
    @Column(length = 1000)
    private String gatewayResponse;

    private LocalDateTime paidAt;

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getGatewayResponse() { return gatewayResponse; }
    public void setGatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
