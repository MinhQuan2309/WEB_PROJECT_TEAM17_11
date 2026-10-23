package com.utetra.module.shipper;

import com.utetra.entity.OrderStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Trang chinh cua khu vuc Shipper (tuan 5).
 *
 * Truoc day chi la khung de san route; nay hien so don theo tung trang thai,
 * so don dang cho nhan va thong ke nhanh trong thang.
 */
@Controller
@RequestMapping("/shipper")
public class ShipperHomeController {

    private final ShipperOrderService shipperOrderService;

    public ShipperHomeController(ShipperOrderService shipperOrderService) {
        this.shipperOrderService = shipperOrderService;
    }

    @GetMapping
    public String dashboard(Authentication authentication, Model model) {
        var counts = shipperOrderService.countByStatus(authentication.getName());
        long dangGiao = counts.get(OrderStatus.CONFIRMED) + counts.get(OrderStatus.PICKED_UP)
                + counts.get(OrderStatus.SHIPPING);

        model.addAttribute("counts", counts);
        model.addAttribute("dangGiaoCount", dangGiao);
        model.addAttribute("availableCount",
                shipperOrderService.listAvailableOrders(0, 1).getTotalElements());
        model.addAttribute("stats", shipperOrderService.stats(authentication.getName(),
                null, null, ShipperOrderService.GroupBy.DAY));
        return "shipper/dashboard";
    }
}