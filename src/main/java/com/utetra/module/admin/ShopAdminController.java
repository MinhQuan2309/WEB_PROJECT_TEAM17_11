package com.utetra.module.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Kiem duyet shop, pham vi toan san. SecurityConfig da gioi han /admin/** cho ADMIN.
 */
@Controller
@RequestMapping("/admin/shops")
public class ShopAdminController extends ShopModerationControllerSupport {

    public ShopAdminController(ShopModerationService moderationService) {
        super(moderationService);
    }

    @Override
    protected String basePath() {
        return "/admin/shops";
    }

    @Override
    protected boolean canAssignManager() {
        return true;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       Principal principal, Model model) {
        return renderList(principal.getName(), status, model);
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(required = false) String status,
                          Principal principal, RedirectAttributes ra) {
        return act(principal.getName(), status,
                actor -> moderationService.approve(actor, id),
                "Da duyet shop. Chu shop duoc nang len VENDOR.", ra);
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String reason,
                         @RequestParam(required = false) String status,
                         Principal principal, RedirectAttributes ra) {
        return act(principal.getName(), status,
                actor -> moderationService.reject(actor, id, reason),
                "Da tu choi ho so.", ra);
    }

    @PostMapping("/{id}/suspend")
    public String suspend(@PathVariable Long id,
                          @RequestParam(required = false) String reason,
                          @RequestParam(required = false) String status,
                          Principal principal, RedirectAttributes ra) {
        return act(principal.getName(), status,
                actor -> moderationService.suspend(actor, id, reason),
                "Da tam dung shop.", ra);
    }

    @PostMapping("/{id}/reactivate")
    public String reactivate(@PathVariable Long id,
                             @RequestParam(required = false) String status,
                             Principal principal, RedirectAttributes ra) {
        return act(principal.getName(), status,
                actor -> moderationService.reactivate(actor, id),
                "Da mo lai shop.", ra);
    }

    @PostMapping("/{id}/reopen")
    public String reopen(@PathVariable Long id,
                         @RequestParam(required = false) String status,
                         Principal principal, RedirectAttributes ra) {
        return act(principal.getName(), status,
                actor -> moderationService.reopen(actor, id),
                "Da mo lai ho so, shop quay ve trang thai cho duyet.", ra);
    }

    @PostMapping("/{id}/manager")
    public String assignManager(@PathVariable Long id,
                                @RequestParam(required = false) Long managerId,
                                @RequestParam(required = false) String status,
                                Principal principal, RedirectAttributes ra) {
        return act(principal.getName(), status,
                actor -> moderationService.assignManager(actor, id, managerId),
                "Da cap nhat nguoi quan ly.", ra);
    }
}
