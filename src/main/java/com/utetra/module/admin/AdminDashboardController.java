package com.utetra.module.admin;

import com.utetra.entity.ShopStatus;
import com.utetra.repository.OrderRepository;
import com.utetra.repository.ProductRepository;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Khu vuc Admin. Tuan 1 chi co dashboard dem so lieu de kiem tra phan quyen.
 * Tuan 4 Quan lam tiep: quan ly user, danh muc, chiet khau, khuyen mai, nha van chuyen.
 */
@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public AdminDashboardController(UserRepository userRepository,
                                    ShopRepository shopRepository,
                                    ProductRepository productRepository,
                                    OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("userCount", userRepository.count());
        model.addAttribute("shopCount", shopRepository.count());
        model.addAttribute("productCount", productRepository.count());
        model.addAttribute("orderCount", orderRepository.count());
        model.addAttribute("revenue", orderRepository.revenueOfPlatform());
        model.addAttribute("pendingShopCount", shopRepository.countByStatus(ShopStatus.PENDING));
        return "admin/dashboard";
    }
}
