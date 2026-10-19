package com.utetra.module.vendor;

import com.utetra.entity.Shop;
import com.utetra.entity.ShopStatus;
import com.utetra.repository.ProductRepository;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.ToppingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/**
 * Trang cong khai cua shop: /shop/{id} (khach xem, khong can dang nhap).
 *
 * Chi hien shop dang ACTIVE. Shop PENDING hoac SUSPENDED tra 404 de khong lo
 * thong tin shop chua duoc duyet ra ngoai.
 */
@Controller
public class ShopPublicController {

    private static final int PRODUCTS_PER_PAGE = 12;

    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final ToppingRepository toppingRepository;

    public ShopPublicController(ShopRepository shopRepository,
                                ProductRepository productRepository,
                                ToppingRepository toppingRepository) {
        this.shopRepository = shopRepository;
        this.productRepository = productRepository;
        this.toppingRepository = toppingRepository;
    }

    @GetMapping("/shop/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Shop shop = shopRepository.findById(id)
                .filter(s -> s.getStatus() == ShopStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay shop"));

        model.addAttribute("shop", shop);
        model.addAttribute("products",
                productRepository.findByShopAndActiveTrue(shop, PageRequest.of(0, PRODUCTS_PER_PAGE)));
        model.addAttribute("toppings", toppingRepository.findByShopAndActiveTrue(shop));
        return "shop/detail";
    }
}