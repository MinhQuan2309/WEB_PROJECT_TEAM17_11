package com.utetra.module.admin;

import com.utetra.entity.Role;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/** Tim kiem va quan ly nguoi dung. SecurityConfig da gioi han /admin/** cho ADMIN. */
@Controller
@RequestMapping("/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Role role,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("page", userAdminService.search(keyword, role, page));
        model.addAttribute("keyword", keyword);
        model.addAttribute("role", role);
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @PostMapping("/{id}/lock")
    public String toggleLock(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            var user = userAdminService.toggleLock(principal.getName(), id);
            ra.addFlashAttribute("successMessage",
                    user.isLocked() ? "Da khoa tai khoan " + user.getUsername()
                                    : "Da mo khoa tai khoan " + user.getUsername());
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/role")
    public String changeRole(@PathVariable Long id,
                             @RequestParam Role role,
                             Principal principal,
                             RedirectAttributes ra) {
        try {
            var user = userAdminService.changeRole(principal.getName(), id, role);
            ra.addFlashAttribute("successMessage",
                    "Da doi vai tro cua " + user.getUsername() + " thanh " + user.getRole());
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }
}
