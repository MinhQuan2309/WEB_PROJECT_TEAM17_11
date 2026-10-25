package com.utetra.module.review;

import com.utetra.entity.*;
import com.utetra.module.account.AccountService;
import com.utetra.module.account.dto.AddressFormRequest;
import com.utetra.module.cart.CartService;
import com.utetra.module.cart.dto.AddToCartRequest;
import com.utetra.module.catalog.CatalogService;
import com.utetra.module.checkout.CheckoutService;
import com.utetra.module.checkout.dto.CheckoutRequest;
import com.utetra.module.review.dto.ReviewFormRequest;
import com.utetra.repository.OrderRepository;
import com.utetra.repository.ProductRepository;
import com.utetra.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiem thu luong Tuan 5 cua Hai: Wishlist, lich su xem san pham (ViewHistory)
 * va danh gia 1-5 sao kem binh luan >= 50 ky tu cho san pham da mua.
 */
@SpringBootTest
@Transactional
class ReviewAndWishlistServiceTest {

    @Autowired ReviewService reviewService;
    @Autowired AccountService accountService;
    @Autowired CatalogService catalogService;
    @Autowired CartService cartService;
    @Autowired CheckoutService checkoutService;
    @Autowired ProductRepository productRepository;
    @Autowired ProductVariantRepository variantRepository;
    @Autowired OrderRepository orderRepository;

    @Test
    void luuWishlistLichSuXemVaDanhGiaSanPhamDaMua() {
        User khach = accountService.getRequiredUser("khach");
        cartService.clearCart(khach);

        Product product = productRepository.findAll().get(0);
        ProductVariant variant = variantRepository.findByProduct(product).get(0);

        // Kiem tra ghi nhan lich su xem va bat/tat yeu thich
        catalogService.recordProductView(product, khach);
        assertFalse(accountService.getRecentlyViewed(khach).isEmpty());
        assertTrue(accountService.toggleWishlist(khach, product.getId()));
        assertFalse(accountService.getWishlistPage(khach, 0, 10).isEmpty());

        // Tao don hang cho san pham
        AddToCartRequest addReq = new AddToCartRequest();
        addReq.setProductId(product.getId());
        addReq.setVariantId(variant.getId());
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
        Order order = checkoutService.placeOrders(khach, checkoutReq).get(0);

        ReviewFormRequest reviewReq = new ReviewFormRequest();
        reviewReq.setProductId(product.getId());
        reviewReq.setOrderId(order.getId());
        reviewReq.setRating(5);
        reviewReq.setContent("Tra sua rat thom ngon, tran chau deo mem, giao hang nhanh dung hen, se ung ho shop nhieu lan nua!");

        // Don chua DELIVERED thi khong duoc phep danh gia
        assertThrows(IllegalStateException.class,
                () -> reviewService.createReview(khach, reviewReq, null));

        // Chuyen don sang DELIVERED va kiem tra chan binh luan ngan hon 50 ky tu
        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        ReviewFormRequest shortReq = new ReviewFormRequest();
        shortReq.setProductId(product.getId());
        shortReq.setOrderId(order.getId());
        shortReq.setRating(5);
        shortReq.setContent("Ngon lam");
        assertThrows(IllegalArgumentException.class,
                () -> reviewService.createReview(khach, shortReq, null));

        // Binh luan hop le >= 50 ky tu
        Review savedReview = reviewService.createReview(khach, reviewReq, null);
        assertNotNull(savedReview.getId());
        assertEquals(5, savedReview.getRating());
    }
}
