package com.utetra.module.vendor;

import com.utetra.entity.Order;
import com.utetra.entity.OrderStatus;
import com.utetra.repository.PaymentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Quan ly don hang cua shop theo 7 trang thai (tuan 3).
 *
 * Nut doi trang thai trong giao dien chi hien cac buoc di tiep hop le, nhung Server
 * van kiem tra lai bang OrderStatusTransitions: khong tin du lieu tu trinh duyet.
 */
@Controller
@RequestMapping("/vendor/orders")
public class VendorOrderController {

    private static final int PAGE_SIZE = 10;

    private final VendorOrderService vendorOrderService;
    private final PaymentRepository paymentRepository;

    public VendorOrderController(VendorOrderService vendorOrderService,
                                 PaymentRepository paymentRepository) {
        this.vendorOrderService = vendorOrderService;
        this.paymentRepository = paymentRepository;
    }

    @GetMapping
    public String list(@RequestParam(value = "status", required = false) String status,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       Authentication authentication,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        OrderStatus loc = null;
        if (status != null && !status.isBlank()) {
            loc = parseStatus(status);
            if (loc == null) {
                redirectAttributes.addFlashAttribute("formError", "Trang thai don khong hop le: " + status);
                return "redirect:/vendor/orders";
            }
        }

        model.addAttribute("orders",
                vendorOrderService.listMyOrders(authentication.getName(), loc, page, PAGE_SIZE));
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("statusFilter", loc);
        model.addAttribute("counts", vendorOrderService.countByStatus(authentication.getName()));
        model.addAttribute("nextStates", nextStatesMap());
        return "vendor/orders";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        Order order = vendorOrderService.requireOwnedOrder(authentication.getName(), id);
        model.addAttribute("order", order);
        model.addAttribute("items", vendorOrderService.itemsOf(order));
        model.addAttribute("shipment", vendorOrderService.shipmentOf(order));
        model.addAttribute("payment", paymentRepository.findTopByOrderOrderByIdDesc(order).orElse(null));
        model.addAttribute("nextStates", vendorOrderService.nextStatesForVendor(order.getStatus()));
        return "vendor/order-detail";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam("status") String status,
                               @RequestParam(value = "note", required = false) String note,
                               @RequestParam(value = "returnTo", required = false) String returnTo,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {

        OrderStatus moi = parseStatus(status);
        if (moi == null) {
            redirectAttributes.addFlashAttribute("formError", "Trang thai don khong hop le: " + status);
            return "redirect:/vendor/orders/" + id;
        }

        try {
            Order order = vendorOrderService.changeStatus(authentication.getName(), id, moi, note);
            redirectAttributes.addFlashAttribute("message",
                    "Don " + order.getCode() + " da chuyen sang: " + order.getStatus().getLabel());
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("formError", ex.getMessage());
        }

        return "list".equals(returnTo) ? "redirect:/vendor/orders" : "redirect:/vendor/orders/" + id;
    }

    private OrderStatus parseStatus(String value) {
        try {
            return OrderStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** Buoc di tiep hop le cua tung trang thai, de giao dien chi hien nut dung luat. */
    private Map<OrderStatus, List<OrderStatus>> nextStatesMap() {
        Map<OrderStatus, List<OrderStatus>> map = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            map.put(status, vendorOrderService.nextStatesForVendor(status));
        }
        return map;
    }
}