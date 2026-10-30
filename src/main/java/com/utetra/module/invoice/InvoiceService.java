package com.utetra.module.invoice;

import com.utetra.entity.Order;
import com.utetra.entity.PaymentStatus;
import com.utetra.repository.PaymentRepository;
import com.utetra.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Hoa don dung chung cho ca khach hang va chu cua hang.
 *
 * Diem giao thu tu giua cac phan: khach xem hoa don o /orders/{id}/invoice,
 * chu cua hang xem o /vendor/orders/{id}/invoice. Hai ben kiem tra quyen theo
 * cach rieng cua minh roi cung goi lop nay de dung noi dung, nen hoa don hai
 * ben nhin thay luon khop nhau.
 */
@Service
public class InvoiceService {

    private final ShipmentRepository shipmentRepository;
    private final PaymentRepository paymentRepository;

    public InvoiceService(ShipmentRepository shipmentRepository,
                          PaymentRepository paymentRepository) {
        this.shipmentRepository = shipmentRepository;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Hoa don chi xuat khi don da thanh toan: don COD la sau khi shipper giao xong,
     * don tra truoc la ngay sau khi cong thanh toan xac nhan.
     */
    public void requirePaid(Order order) {
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException(
                    "Đơn này chưa thanh toán nên chưa xuất được hóa đơn. "
                    + "Đơn thanh toán khi nhận hàng sẽ có hóa đơn sau khi shipper giao xong.");
        }
    }

    /**
     * Lich su nhan don in kem hoa don: dat hang, shipper nhan don, lay hang,
     * giao thanh cong va thoi diem thanh toan. Moc nao chua co thi bo qua.
     */
    @Transactional(readOnly = true)
    public List<InvoiceStep> timelineOf(Order order) {
        List<InvoiceStep> moc = new ArrayList<>();
        moc.add(new InvoiceStep("Khách đặt hàng", order.getCreatedAt(),
                "Đơn " + order.getCode() + " tại " + order.getShop().getName()));

        shipmentRepository.findByOrder(order).ifPresent(shipment -> {
            String tenShipper = shipment.getShipper() != null
                    ? shipment.getShipper().getFullName()
                    : "chưa rõ";
            if (shipment.getAssignedAt() != null) {
                moc.add(new InvoiceStep("Shipper nhận đơn", shipment.getAssignedAt(),
                        tenShipper + " · mã vận đơn " + shipment.getTrackingCode()));
            }
            if (shipment.getPickedUpAt() != null) {
                moc.add(new InvoiceStep("Lấy hàng tại cửa hàng", shipment.getPickedUpAt(),
                        tenShipper));
            }
            if (shipment.getDeliveredAt() != null) {
                moc.add(new InvoiceStep("Giao thành công", shipment.getDeliveredAt(),
                        "Người nhận: " + order.getReceiverName()));
            }
        });

        paymentRepository.findByOrder(order).stream()
                .filter(tt -> tt.getStatus() == PaymentStatus.PAID && tt.getPaidAt() != null)
                .forEach(tt -> moc.add(new InvoiceStep("Thanh toán", tt.getPaidAt(),
                        tt.getMethod().getLabel()
                        + (tt.getTransactionId() != null
                           ? " · mã giao dịch " + tt.getTransactionId() : ""))));

        moc.sort(Comparator.comparing(InvoiceStep::thoiDiem,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return moc;
    }

    /** Mot moc trong lich su nhan don, dung de in ra hoa don. */
    public record InvoiceStep(String ten, LocalDateTime thoiDiem, String ghiChu) {
    }
}
