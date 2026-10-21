package com.utetra.module.cart;

import com.utetra.entity.*;
import com.utetra.module.account.AccountService;
import com.utetra.module.account.dto.AddressFormRequest;
import com.utetra.module.cart.dto.AddToCartRequest;
import com.utetra.module.cart.dto.CartSummaryDto;
import com.utetra.module.checkout.CheckoutService;
import com.utetra.module.checkout.dto.CheckoutRequest;
import com.utetra.repository.ProductRepository;
import com.utetra.repository.ProductVariantRepository;
import com.utetra.repository.ToppingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiem thu tu dong Tuan 3 cua Hai: Gio hang luu CSDL (cong don khi trung tuy chon, nhom theo Shop)
 * va Thanh toan (Checkout COD, ap ma Voucher, tach don theo Shop).
 */
@SpringBootTest
@Transactional
class CartAndCheckoutServiceTest {

    @Autowired CartService cartService;
    @Autowired CheckoutService checkoutService;
    @Autowired AccountService accountService;
    @Autowired ProductRepository productRepository;
    @Autowired ProductVariantRepository variantRepository;
    @Autowired ToppingRepository toppingRepository;

    @Test
    void themVaoGioHangCongDonKhiTrungTuyChonVaDatHangCodThanhCong() {
        User khach = cartService.getRequiredUser("khach");
        cartService.clearCart(khach);

        Product product = productRepository.findAll().get(0);
        List<ProductVariant> variants = variantRepository.findByProduct(product);
        List<Topping> toppings = toppingRepository.findByShopAndActiveTrue(product.getShop());

        AddToCartRequest req = new AddToCartRequest();
        req.setProductId(product.getId());
        req.setVariantId(variants.get(0).getId());
        req.setToppingIds(List.of(toppings.get(0).getId()));
        req.setQuantity(2);
        req.setSugarLevel(70);
        req.setIceLevel(50);

        cartService.addToCart(khach, req);
        // Them lan 2 cung tuy chon -> phai cong don len 4 ly tren cung 1 dong CartItem
        cartService.addToCart(khach, req);

        CartSummaryDto summary = cartService.getCartSummary(khach, null);
        assertFalse(summary.isEmpty());
        assertEquals(1, summary.totalLines());
        assertEquals(4, summary.totalQuantity());

        // Tao dia chi va dat hang COD kem ma Voucher UTETRA10
        AddressFormRequest addrReq = new AddressFormRequest();
        addrReq.setReceiverName("Hoang Van Khach");
        addrReq.setPhone("0901234567");
        addrReq.setStreet("01 Vo Van Ngan");
        addrReq.setWard("Linh Chieu");
        addrReq.setDistrict("Thu Duc");
        addrReq.setProvince("TP.HCM");
        addrReq.setDefaultAddress(true);
        Address savedAddr = accountService.saveAddress(khach, addrReq);

        checkoutService.ensureSampleVouchersOnDemand();
        CheckoutRequest checkoutReq = new CheckoutRequest();
        checkoutReq.setAddressId(savedAddr.getId());
        checkoutReq.setVoucherCode("UTETRA10");
        checkoutReq.setPaymentMethod(PaymentMethod.COD);
        checkoutReq.setNote("Giao nhanh giup minh");

        List<Order> createdOrders = checkoutService.placeOrders(khach, checkoutReq);
        assertEquals(1, createdOrders.size());
        Order order = createdOrders.get(0);
        assertEquals(OrderStatus.NEW, order.getStatus());
        assertEquals(PaymentMethod.COD, order.getPaymentMethod());
        assertTrue(order.getDiscount().signum() > 0);

        // Sau khi dat hang xong, gio hang phai duoc lam trong
        assertTrue(cartService.getCartSummary(khach, null).isEmpty());
    }
}
