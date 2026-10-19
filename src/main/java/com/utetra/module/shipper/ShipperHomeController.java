package com.utetra.module.shipper;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * KHUNG CHO THIEN. Chi de san route /shipper da duoc phan quyen.
 *
 * Thien lam tiep: danh sach don duoc phan cong giao
 * (OrderRepository.findByShipperAndStatusOrderByIdDesc), cap nhat trang thai,
 * thong ke don da giao / dang giao / that bai.
 */
@Controller
@RequestMapping("/shipper")
public class ShipperHomeController {

    @GetMapping
    public String dashboard() {
        return "shipper/dashboard";
    }
}
