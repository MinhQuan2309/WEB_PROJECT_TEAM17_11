package com.utetra.module.shipper;

import com.utetra.entity.Order;
import com.utetra.entity.OrderStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Man hinh shipper: nhan don, cap nhat trang thai, thong ke (tuan 5).
 *
 *   GET  /shipper/orders              : don cua toi, loc theo trang thai
 *   GET  /shipper/orders/{id}         : chi tiet don dang giao
 *   POST /shipper/orders/{id}/status  : da lay hang / dang giao / giao xong / that bai
 *   GET  /shipper/available           : don cho nhan
 *   POST /shipper/available/{id}/claim: nhan don
 *   GET  /shipper/stats               : thong ke theo ngay hoac thang
 */
@Controller
@RequestMapping("/shipper")
public class ShipperOrderController {

    private static final int PAGE_SIZE = 10;

    private final ShipperOrderService shipperOrderService;

    public ShipperOrderController(ShipperOrderService shipperOrderService) {
        this.shipperOrderService = shipperOrderService;
    }

    @GetMapping("/orders")
    public String orders(@RequestParam(value = "status", required = false) String status,
                         @RequestParam(value = "page", defaultValue = "0") int page,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        OrderStatus loc = null;
        if (status != null && !status.isBlank()) {
            loc = parseStatus(status);
            if (loc == null) {
                redirectAttributes.addFlashAttribute("formError", "Trang thai khong hop le: " + status);
                return "redirect:/shipper/orders";
            }
        }

        model.addAttribute("orders",
                shipperOrderService.listMyDeliveries(authentication.getName(), loc, page, PAGE_SIZE));
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("statusFilter", loc);
        model.addAttribute("counts", shipperOrderService.countByStatus(authentication.getName()));
        model.addAttribute("nextStates", nextStatesMap());
        return "shipper/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Authentication authentication, Model model) {
        Order order = shipperOrderService.requireAssignedOrder(authentication.getName(), id);
        model.addAttribute("order", order);
        model.addAttribute("items", shipperOrderService.itemsOf(order));
        model.addAttribute("shipment", shipperOrderService.shipmentOf(order));
        model.addAttribute("codAmount", shipperOrderService.codAmountOf(order));
        model.addAttribute("codRequired",
                shipperOrderService.codAmountOf(order).compareTo(java.math.BigDecimal.ZERO) > 0);
        model.addAttribute("nextStates", shipperOrderService.nextStatesForShipper(order.getStatus()));
        return "shipper/order-detail";
    }

    @PostMapping("/orders/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam("status") String status,
                               @RequestParam(value = "note", required = false) String note,
                               @RequestParam(value = "failedReason", required = false) String failedReason,
                               @RequestParam(value = "returnTo", required = false) String returnTo,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {

        OrderStatus moi = parseStatus(status);
        if (moi == null) {
            redirectAttributes.addFlashAttribute("formError", "Trang thai khong hop le: " + status);
            return "redirect:/shipper/orders/" + id;
        }

        try {
            Order order = shipperOrderService.updateStatus(authentication.getName(), id, moi,
                    note, failedReason);
            redirectAttributes.addFlashAttribute("message",
                    "Don " + order.getCode() + " da chuyen sang: " + order.getStatus().getLabel());
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("formError", ex.getMessage());
        }

        return "list".equals(returnTo) ? "redirect:/shipper/orders" : "redirect:/shipper/orders/" + id;
    }

    @GetMapping("/available")
    public String available(@RequestParam(value = "page", defaultValue = "0") int page, Model model) {
        model.addAttribute("orders", shipperOrderService.listAvailableOrders(page, PAGE_SIZE));
        return "shipper/available";
    }

    @PostMapping("/available/{id}/claim")
    public String claim(@PathVariable Long id, Authentication authentication,
                        RedirectAttributes redirectAttributes) {
        try {
            Order order = shipperOrderService.claimOrder(authentication.getName(), id);
            redirectAttributes.addFlashAttribute("message",
                    "Da nhan don " + order.getCode() + ". Xem o muc \"Don cua toi\".");
            return "redirect:/shipper/orders/" + order.getId();
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("formError", ex.getMessage());
            return "redirect:/shipper/available";
        }
    }

    @GetMapping("/stats")
    public String stats(@RequestParam(value = "from", required = false)
                        @org.springframework.format.annotation.DateTimeFormat(iso =
                                org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                        LocalDate from,
                        @RequestParam(value = "to", required = false)
                        @org.springframework.format.annotation.DateTimeFormat(iso =
                                org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                        LocalDate to,
                        @RequestParam(value = "groupBy", required = false) String groupBy,
                        Authentication authentication,
                        Model model) {

        ShipperOrderService.GroupBy kieuGop = parseGroupBy(groupBy);
        model.addAttribute("stats",
                shipperOrderService.stats(authentication.getName(), from, to, kieuGop));
        model.addAttribute("groupBy", kieuGop.name());
        model.addAttribute("counts", shipperOrderService.countByStatus(authentication.getName()));
        model.addAttribute("availableCount",
                shipperOrderService.listAvailableOrders(0, 1).getTotalElements());
        return "shipper/stats";
    }

    private OrderStatus parseStatus(String value) {
        try {
            return OrderStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private ShipperOrderService.GroupBy parseGroupBy(String value) {
        if (value == null || value.isBlank()) {
            return ShipperOrderService.GroupBy.DAY;
        }
        try {
            return ShipperOrderService.GroupBy.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ShipperOrderService.GroupBy.DAY;
        }
    }

    /** Buoc di tiep hop le cua tung trang thai, de giao dien chi hien nut dung luat. */
    private Map<OrderStatus, List<OrderStatus>> nextStatesMap() {
        Map<OrderStatus, List<OrderStatus>> map = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            map.put(status, shipperOrderService.nextStatesForShipper(status));
        }
        return map;
    }
}