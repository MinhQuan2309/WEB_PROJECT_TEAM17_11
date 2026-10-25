package com.utetra.module.manager;

import com.utetra.module.admin.ShopModerationControllerSupport;
import com.utetra.module.admin.ShopModerationService;
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
 * Kiem duyet shop trong pham vi cua Manager: shop minh dang quan ly,
 * cong them shop chua ai nhan. Duyet mot shop chua ai nhan thi nhan luon shop do.
 *
 * Dung chung ShopModerationService voi khu Admin, chi khac pham vi va
 * khong co quyen phan cong nguoi quan ly.
 */
@Controller
@RequestMapping("/manager/shops")
public class ShopManagerController extends ShopModerationControllerSupport {

    public ShopManagerController(ShopModerationService moderationService) {
        super(moderationService);
    }

    @Override
    protected String basePath() {
        return "/manager/shops";
    }

    @Override
    protected boolean canAssignManager() {
        return false;
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

    @PostMapping("/{id}/commission")
    public String commission(@PathVariable Long id,
                             @RequestParam java.math.BigDecimal rate,
                             @RequestParam(required = false) String status,
                             Principal principal, RedirectAttributes ra) {
        return act(principal.getName(), status,
                actor -> moderationService.setCommissionRate(actor, id, rate),
                "Da cap nhat chiet khau cua shop.", ra);
    }

    @PostMapping("/{id}/reopen")
    public String reopen(@PathVariable Long id,
                         @RequestParam(required = false) String status,
                         Principal principal, RedirectAttributes ra) {
        return act(principal.getName(), status,
                actor -> moderationService.reopen(actor, id),
                "Da mo lai ho so, shop quay ve trang thai cho duyet.", ra);
    }
}
