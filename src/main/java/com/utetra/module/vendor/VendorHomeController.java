package com.utetra.module.vendor;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * KHUNG CHO THIEN. Chi de san route /vendor da duoc phan quyen.
 *
 * Thien lam tiep tai module nay:
 *   - dang ky shop, quan ly trang chu shop
 *   - CRUD san pham + size + topping (upload anh qua CloudinaryService)
 *   - quan ly don hang cua shop theo 7 trang thai
 *   - tao chuong trinh khuyen mai, phat hanh voucher cua shop
 *   - bao cao doanh thu shop (OrderRepository.revenueOfShop)
 */
@Controller
@RequestMapping("/vendor")
public class VendorHomeController {

    @GetMapping
    public String dashboard() {
        return "vendor/dashboard";
    }
}
