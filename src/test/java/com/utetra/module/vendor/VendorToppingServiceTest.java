package com.utetra.module.vendor;

import com.utetra.entity.Cart;
import com.utetra.entity.CartItem;
import com.utetra.entity.Product;
import com.utetra.entity.Shop;
import com.utetra.entity.Topping;
import com.utetra.module.vendor.dto.ToppingForm;
import com.utetra.repository.CartItemRepository;
import com.utetra.repository.CartRepository;
import com.utetra.repository.ProductRepository;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.ToppingRepository;
import com.utetra.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem tra nghiep vu topping cua tuan 2: CRUD, chan IDOR, chan xoa topping dang trong gio hang.
 */
@SpringBootTest
@Transactional
class VendorToppingServiceTest {

    @Autowired VendorToppingService toppingService;
    @Autowired ToppingRepository toppingRepository;
    @Autowired CartRepository cartRepository;
    @Autowired CartItemRepository cartItemRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ShopRepository shopRepository;
    @Autowired UserRepository userRepository;

    private Shop shopOf(String username) {
        return shopRepository.findByOwner(userRepository.findByUsername(username).orElseThrow())
                .orElseThrow();
    }

    private Topping toppingDauTienCua(String username) {
        return toppingRepository.findByShopOrderByIdAsc(shopOf(username)).get(0);
    }

    private ToppingForm form(String name, long price) {
        ToppingForm form = new ToppingForm();
        form.setName(name);
        form.setPrice(BigDecimal.valueOf(price));
        form.setActive(true);
        return form;
    }

    @Test
    @DisplayName("Them / sua / tat topping cua shop minh")
    void crudToppingCuaMinh() {
        Topping topping = toppingService.create("vendor1", form("Thach cafe", 7000));
        assertNotNull(topping.getId());
        assertEquals(shopOf("vendor1").getId(), topping.getShop().getId());

        toppingService.update("vendor1", topping.getId(), form("Thach cafe sua", 9000));
        Topping sau = toppingRepository.findById(topping.getId()).orElseThrow();
        assertEquals("Thach cafe sua", sau.getName());
        assertEquals(0, sau.getPrice().compareTo(BigDecimal.valueOf(9000)));

        toppingService.toggleActive("vendor1", topping.getId());
        assertFalse(toppingRepository.findById(topping.getId()).orElseThrow().isActive());
    }

    @Test
    @DisplayName("IDOR: Vendor khac khong sua / xoa / tat duoc topping cua shop minh")
    void vendorKhacKhongChamDuocTopping() {
        Topping cua = toppingDauTienCua("vendor1");
        String tenCu = cua.getName();

        assertThrows(AccessDeniedException.class,
                () -> toppingService.requireOwnedTopping("vendor2", cua.getId()));
        assertThrows(AccessDeniedException.class,
                () -> toppingService.update("vendor2", cua.getId(), form("Ten bi chiem", 1)));
        assertThrows(AccessDeniedException.class,
                () -> toppingService.toggleActive("vendor2", cua.getId()));
        assertThrows(AccessDeniedException.class,
                () -> toppingService.delete("vendor2", cua.getId()));

        assertEquals(tenCu, toppingRepository.findById(cua.getId()).orElseThrow().getName());
    }

    @Test
    @DisplayName("Xoa topping chua ai dat thi xoa duoc")
    void xoaToppingChuaAiDat() {
        Topping topping = toppingService.create("vendor1", form("Topping de xoa", 5000));
        toppingService.delete("vendor1", topping.getId());
        assertFalse(toppingRepository.findById(topping.getId()).isPresent());
    }

    @Test
    @DisplayName("Topping dang nam trong gio hang cua khach thi khong xoa duoc")
    void toppingTrongGioHangThiKhongXoaDuoc() {
        Topping topping = toppingDauTienCua("vendor1");
        Product product = productRepository
                .findByShopOrderByIdDesc(shopOf("vendor1"), PageRequest.of(0, 1))
                .getContent().get(0);

        Cart cart = new Cart();
        cart.setUser(userRepository.findByUsername("khach").orElseThrow());
        cartRepository.save(cart);

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(1);
        item.setToppings(List.of(topping));
        cartItemRepository.save(item);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> toppingService.delete("vendor1", topping.getId()));
        assertTrue(ex.getMessage().contains("Tat"), "Thong bao phai huong dan dung chuc nang Tat");

        assertTrue(toppingRepository.findById(topping.getId()).isPresent());
    }
}