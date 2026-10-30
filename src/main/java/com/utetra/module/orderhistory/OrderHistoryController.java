package com.utetra.module.orderhistory;

import com.utetra.entity.Order;
import com.utetra.entity.OrderStatus;
import com.utetra.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Dieu huong man hinh lich su mua hang, chi tiet don va thao tac huy / tra hang (/orders).
 */
@Controller
@RequestMapping("/orders")
public class OrderHistoryController {

    private static final int PAGE_SIZE = 10;

    private final OrderHistoryService orderHistoryService;

    public OrderHistoryController(OrderHistoryService orderHistoryService) {
        this.orderHistoryService = orderHistoryService;
    }

    /** Danh sach don hang cua khach theo tab trang thai. */
    @GetMapping
    public String listOrders(@RequestParam(required = false) String status,
                             @RequestParam(defaultValue = "0") int page,
                             Principal principal,
                             Model model) {
        User user = orderHistoryService.getRequiredUser(principal.getName());

        OrderStatus filterStatus = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                filterStatus = OrderStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                filterStatus = null;
            }
        }

        Page<Order> orderPage = orderHistoryService.getCustomerOrders(user, filterStatus, page, PAGE_SIZE);

        model.addAttribute("orderPage", orderPage);
        model.addAttribute("selectedStatus", filterStatus != null ? filterStatus.name() : "ALL");
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("statusCounts", orderHistoryService.countCustomerOrdersByStatus(user));
        return "orderhistory/index";
    }

    /** Xem chi tiet mot don hang cua khach. */
    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id,
                              Principal principal,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = orderHistoryService.getRequiredUser(principal.getName());
            Order order = orderHistoryService.getCustomerOrder(user, id);

            model.addAttribute("order", order);
            model.addAttribute("shipment", orderHistoryService.getShipmentForOrder(order).orElse(null));
            model.addAttribute("payments", orderHistoryService.getPaymentsForOrder(order));
            return "orderhistory/detail";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/orders";
        }
    }

    /**
     * Hoa don in duoc kem lich su nhan don. Chi mo duoc khi don da thanh toan:
     * don COD la sau khi shipper giao xong.
     */
    @GetMapping("/{id}/invoice")
    public String invoice(@PathVariable Long id,
                          Principal principal,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        try {
            User user = orderHistoryService.getRequiredUser(principal.getName());
            Order order = orderHistoryService.getInvoiceOrder(user, id);

            model.addAttribute("order", order);
            model.addAttribute("shipment", orderHistoryService.getShipmentForOrder(order).orElse(null));
            model.addAttribute("payments", orderHistoryService.getPaymentsForOrder(order));
            model.addAttribute("timeline", orderHistoryService.timelineOf(order));
            return "orderhistory/invoice";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/orders/" + id;
        }
    }

    /** Xu ly yeu cau huy don hang tu khach. */
    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @RequestParam(required = false) String reason,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = orderHistoryService.getRequiredUser(principal.getName());
            Order cancelled = orderHistoryService.cancelOrder(user, id, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã hủy đơn hàng " + cancelled.getCode() + " thành công.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    /** Xu ly yeu cau tra hang - hoan tien khi don da giao. */
    @PostMapping("/{id}/return")
    public String returnOrder(@PathVariable Long id,
                              @RequestParam String reason,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = orderHistoryService.getRequiredUser(principal.getName());
            Order returned = orderHistoryService.requestReturnRefund(user, id, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã gửi yêu cầu trả hàng – hoàn tiền cho đơn " + returned.getCode() + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }
}
