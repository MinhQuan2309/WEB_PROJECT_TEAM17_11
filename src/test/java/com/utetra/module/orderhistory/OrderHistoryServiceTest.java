package com.utetra.module.orderhistory;

import com.utetra.entity.*;
import com.utetra.module.account.AccountService;
import com.utetra.module.account.dto.AddressFormRequest;
import com.utetra.module.cart.CartService;
import com.utetra.module.cart.dto.AddToCartRequest;
import com.utetra.module.checkout.CheckoutService;
import com.utetra.module.checkout.dto.CheckoutRequest;
import com.utetra.repository.OrderRepository;
import com.utetra.repository.ProductRepository;
import com.utetra.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiem thu luong lich su don hang, huy don khi NEW va tra hang - hoan tien khi DELIVERED.
 */
@SpringBootTest
@Transactional
class OrderHistoryServiceTest {

    @Autowired OrderHistoryService orderHistoryService;
    @Autowired CartService cartService;
    @Autowired CheckoutService checkoutService;
    @Autowired AccountService accountService;
    @Autowired ProductRepository productRepository;
    @Autowired ProductVariantRepository variantRepository;
    @Autowired OrderRepository orderRepository;

    @Test
    void xemLichSuDonHuyDonVaTraHangHoanTien() {
        User khach = orderHistoryService.getRequiredUser("khach");
        cartService.clearCart(khach);

        Product p = productRepository.findAll().get(0);
        ProductVariant v = variantRepository.findByProduct(p).get(0);

        AddToCartRequest addReq = new AddToCartRequest();
        addReq.setProductId(p.getId());
        addReq.setVariantId(v.getId());
        addReq.setQuantity(1);
        cartService.addToCart(khach, addReq);

        AddressFormRequest addrReq = new AddressFormRequest();
        addrReq.setReceiverName("Hoang Van Khach");
        addrReq.setPhone("0901234567");
        addrReq.setStreet("01 Vo Van Ngan");
        addrReq.setWard("Linh Chieu");
        addrReq.setDistrict("Thu Duc");
        addrReq.setProvince("TP.HCM");
        addrReq.setDefaultAddress(true);
        Address savedAddr = accountService.saveAddress(khach, addrReq);

        CheckoutRequest checkoutReq = new CheckoutRequest();
        checkoutReq.setAddressId(savedAddr.getId());
        checkoutReq.setPaymentMethod(PaymentMethod.COD);
        List<Order> orders = checkoutService.placeOrders(khach, checkoutReq);
        assertEquals(1, orders.size());
        Order order = orders.get(0);

        Map<String, Long> counts = orderHistoryService.countCustomerOrdersByStatus(khach);
        assertTrue(counts.get("ALL") >= 1);
        assertTrue(counts.get("NEW") >= 1);

        Page<Order> newOrders = orderHistoryService.getCustomerOrders(khach, OrderStatus.NEW, 0, 10);
        assertFalse(newOrders.isEmpty());

        Order cancelled = orderHistoryService.cancelOrder(khach, order.getId(), "Doi y khong uong nua");
        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        assertTrue(cancelled.getNote().contains("Doi y khong uong nua"));

        cartService.addToCart(khach, addReq);
        List<Order> orders2 = checkoutService.placeOrders(khach, checkoutReq);
        Order order2 = orders2.get(0);
        order2.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order2);

        Order returned = orderHistoryService.requestReturnRefund(khach, order2.getId(), "Giao sai vi tra sua, yeu cau tra hang");
        assertEquals(OrderStatus.RETURNED_REFUNDED, returned.getStatus());
        assertTrue(returned.getNote().contains("Giao sai vi tra sua"));
    }

    // ============================================== hoa don

    /** Dung lai cac buoc dat hang cua bai tren de co mot don COD moi. */
    private Order datMotDonCod(User khach) {
        cartService.clearCart(khach);

        Product p = productRepository.findAll().get(0);
        ProductVariant v = variantRepository.findByProduct(p).get(0);

        AddToCartRequest addReq = new AddToCartRequest();
        addReq.setProductId(p.getId());
        addReq.setVariantId(v.getId());
        addReq.setQuantity(1);
        cartService.addToCart(khach, addReq);

        AddressFormRequest addrReq = new AddressFormRequest();
        addrReq.setReceiverName("Hoang Van Khach");
        addrReq.setPhone("0901234567");
        addrReq.setStreet("01 Vo Van Ngan");
        addrReq.setWard("Linh Chieu");
        addrReq.setDistrict("Thu Duc");
        addrReq.setProvince("TP.HCM");
        addrReq.setDefaultAddress(true);
        Address savedAddr = accountService.saveAddress(khach, addrReq);

        CheckoutRequest req = new CheckoutRequest();
        req.setAddressId(savedAddr.getId());
        req.setPaymentMethod(PaymentMethod.COD);
        return checkoutService.placeOrders(khach, req).get(0);
    }

    @Test
    void donChuaThanhToanThiChuaXuatDuocHoaDon() {
        User khach = orderHistoryService.getRequiredUser("khach");
        Order don = datMotDonCod(khach);

        assertEquals(PaymentStatus.PENDING, don.getPaymentStatus());
        assertThrows(IllegalStateException.class,
                () -> orderHistoryService.getInvoiceOrder(khach, don.getId()));
    }

    @Test
    void donCodDaGiaoXongThiXuatDuocHoaDonKemLichSu() {
        User khach = orderHistoryService.getRequiredUser("khach");
        Order don = datMotDonCod(khach);

        // Shipper giao xong: he thong tu danh dau da thanh toan cho don COD.
        don.setStatus(OrderStatus.DELIVERED);
        don.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(don);

        Order hoaDon = orderHistoryService.getInvoiceOrder(khach, don.getId());
        assertEquals(don.getId(), hoaDon.getId());

        var lichSu = orderHistoryService.timelineOf(hoaDon);
        assertFalse(lichSu.isEmpty(), "Hoa don phai co it nhat moc dat hang");
        assertEquals("Khách đặt hàng", lichSu.get(0).ten());
    }

    @Test
    void khongXemDuocHoaDonCuaNguoiKhac() {
        User khach = orderHistoryService.getRequiredUser("khach");
        User nguoiKhac = orderHistoryService.getRequiredUser("vendor1");
        Order don = datMotDonCod(khach);
        don.setStatus(OrderStatus.DELIVERED);
        don.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(don);

        assertThrows(Exception.class,
                () -> orderHistoryService.getInvoiceOrder(nguoiKhac, don.getId()));
    }
}
