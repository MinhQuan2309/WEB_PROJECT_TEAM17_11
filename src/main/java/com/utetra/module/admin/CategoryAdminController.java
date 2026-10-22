package com.utetra.module.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** CRUD danh muc san pham. */
@Controller
@RequestMapping("/admin/categories")
public class CategoryAdminController {

    private final CategoryAdminService categoryAdminService;

    public CategoryAdminController(CategoryAdminService categoryAdminService) {
        this.categoryAdminService = categoryAdminService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryAdminService.findAll());
        model.addAttribute("roots", categoryAdminService.findRoots());
        return "admin/categories";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String name,
                       @RequestParam(required = false) Long parentId,
                       @RequestParam(required = false) String imageUrl,
                       RedirectAttributes ra) {
        try {
            categoryAdminService.save(id, name, parentId, imageUrl);
            ra.addFlashAttribute("successMessage",
                    id == null ? "Da them danh muc moi." : "Da cap nhat danh muc.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        try {
            var c = categoryAdminService.toggleActive(id);
            ra.addFlashAttribute("successMessage",
                    c.isActive() ? "Da bat danh muc " + c.getName() : "Da tat danh muc " + c.getName());
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            categoryAdminService.delete(id);
            ra.addFlashAttribute("successMessage", "Da xoa danh muc.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
