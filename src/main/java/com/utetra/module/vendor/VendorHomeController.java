package com.utetra.module.vendor;

import com.utetra.entity.OrderStatus;
import com.utetra.entity.Shop;
import com.utetra.repository.OrderRepository;
import com.utetra.repository.ProductRepository;
import com.utetra.repository.ToppingRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.EnumMap;
import java.util.Map;

/**
 * Dashboard khu Vendor (tuan 1: thong tin shop + so lieu tong quan).
 *
 * Cac tuan sau bo sung dan vao trang nay: danh sach san pham (tuan 2),
 * don hang theo 7 trang thai (tuan 3), doanh thu chi tiet (tuan 5).
 */
@Controller
@RequestMapping("/vendor")
public class VendorHomeController {

    private final VendorShopService vendorShopService;
    private final ProductRepository productRepository;
    private final ToppingRepository toppingRepository;
    private final OrderRepository orderRepository;

    public VendorHomeController(VendorShopService vendorShopService,
                                ProductRepository productRepository,
                                ToppingRepository toppingRepository,
                                OrderRepository orderRepository) {
        this.vendorShopService = vendorShopService;
        this.productRepository = productRepository;
        this.toppingRepository = toppingRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping
    public String dashboard(Authentication authentication, Model model) {
        Shop shop = vendorShopService.requireMyShop(authentication.getName());

        Map<OrderStatus, Long> orderCounts = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            orderCounts.put(status, orderRepository.countByShopAndStatus(shop, status));
        }

        model.addAttribute("shop", shop);
        model.addAttribute("productCount", productRepository.countByShop(shop));
        model.addAttribute("activeProductCount", productRepository.countByShopAndActiveTrue(shop));
        model.addAttribute("toppingCount", toppingRepository.countByShop(shop));
        model.addAttribute("orderCount", orderRepository.countByShop(shop));
        model.addAttribute("orderCounts", orderCounts);
        model.addAttribute("revenue", orderRepository.revenueOfShop(shop));
        return "vendor/dashboard";
    }
}