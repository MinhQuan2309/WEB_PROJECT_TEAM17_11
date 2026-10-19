package com.utetra.module.manager;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Khu vuc Manager: cung bo chuc nang voi Admin nhung chi trong pham vi
 * cac shop minh duoc phan cong (Shop.manager). Quan lam tiep o tuan 4.
 */
@Controller
@RequestMapping("/manager")
public class ManagerDashboardController {

    @GetMapping
    public String dashboard() {
        return "manager/dashboard";
    }
}
