package com.utetra.module.admin;

import com.utetra.entity.Shop;
import com.utetra.entity.ShopStatus;
import com.utetra.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.function.Consumer;

/**
 * Phan dung chung cho hai man kiem duyet shop (/admin/shops va /manager/shops).
 * Hai controller chi khac nhau o duong dan goc va o chuyen co duoc phan cong
 * nguoi quan ly hay khong, nen gom logic lap lai vao day.
 */
public abstract class ShopModerationControllerSupport {

    protected final ShopModerationService moderationService;

    protected ShopModerationControllerSupport(ShopModerationService moderationService) {
        this.moderationService = moderationService;
    }

    /** Duong dan goc cua man hinh, vi du "/admin/shops". */
    protected abstract String basePath();

    /** Admin duoc phan cong nguoi quan ly, Manager thi khong. */
    protected abstract boolean canAssignManager();

    protected String renderList(String username, String status, Model model) {
        User actor = moderationService.requireActor(username);
        ShopStatus filter = parseStatus(status);
        List<Shop> shops = moderationService.visibleShops(actor, filter);

        model.addAttribute("shops", shops);
        model.addAttribute("currentStatus", filter);
        model.addAttribute("statuses", ShopStatus.values());
        model.addAttribute("basePath", basePath());
        model.addAttribute("canAssignManager", canAssignManager());
        model.addAttribute("pendingCount", moderationService.countByStatus(ShopStatus.PENDING));
        if (canAssignManager()) {
            model.addAttribute("managers", moderationService.allManagers());
        }
        return "admin/shops";
    }

    /**
     * Chay mot hanh dong kiem duyet roi quay ve danh sach kem thong bao.
     * Gom try/catch vao day de bon handler ben duoi khong lap lai.
     */
    protected String act(String username, String status,
                         Consumer<User> action, String successMessage,
                         RedirectAttributes redirectAttributes) {
        try {
            action.accept(moderationService.requireActor(username));
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
        } catch (AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Khong co quyen: " + ex.getMessage());
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        String suffix = (status == null || status.isBlank()) ? "" : "?status=" + status;
        return "redirect:" + basePath() + suffix;
    }

    private ShopStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ShopStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
