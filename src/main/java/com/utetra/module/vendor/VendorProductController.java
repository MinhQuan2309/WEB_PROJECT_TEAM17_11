package com.utetra.module.vendor;

import com.utetra.entity.Product;
import com.utetra.module.vendor.dto.ProductForm;
import com.utetra.module.vendor.dto.VariantForm;
import com.utetra.module.vendor.upload.InvalidImageException;
import com.utetra.repository.CategoryRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * Quan ly san pham cua shop (tuan 2): danh sach, them, sua, tat ban, xoa.
 *
 * Moi thao tac ghi deu di qua VendorProductService va duoc kiem tra chu so huu,
 * nen cac duong dan co {id} o day khong phai la lo hong IDOR.
 */
@Controller
@RequestMapping("/vendor/products")
public class VendorProductController {

    private static final int PAGE_SIZE = 10;

    private final VendorProductService vendorProductService;
    private final CategoryRepository categoryRepository;

    public VendorProductController(VendorProductService vendorProductService,
                                   CategoryRepository categoryRepository) {
        this.vendorProductService = vendorProductService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public String list(@RequestParam(value = "active", required = false) Boolean active,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       Authentication authentication,
                       Model model) {

        var products = vendorProductService.listMyProducts(authentication.getName(), active, page, PAGE_SIZE);

        Map<Long, Integer> variantCounts = new HashMap<>();
        for (Product product : products.getContent()) {
            variantCounts.put(product.getId(), vendorProductService.variantsOf(product).size());
        }

        model.addAttribute("products", products);
        model.addAttribute("variantCounts", variantCounts);
        model.addAttribute("activeFilter", active);
        return "vendor/products";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        ProductForm form = new ProductForm();
        form.getVariants().add(new VariantForm());
        model.addAttribute("form", form);
        model.addAttribute("productId", null);
        addCategories(model);
        return "vendor/product-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ProductForm form,
                         BindingResult bindingResult,
                         @RequestParam(value = "image", required = false) MultipartFile image,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return reRenderForm(model, null);
        }
        try {
            Product product = vendorProductService.create(authentication.getName(), form, image);
            redirectAttributes.addFlashAttribute("message", "Da them san pham: " + product.getName());
        } catch (InvalidImageException ex) {
            model.addAttribute("uploadError", ex.getMessage());
            return reRenderForm(model, null);
        } catch (IllegalStateException ex) {
            model.addAttribute("formError", ex.getMessage());
            return reRenderForm(model, null);
        }
        return "redirect:/vendor/products";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication authentication, Model model) {
        Product product = vendorProductService.requireOwnedProduct(authentication.getName(), id);

        ProductForm form = new ProductForm();
        form.setName(product.getName());
        form.setDescription(product.getDescription());
        form.setBasePrice(product.getBasePrice());
        form.setActive(product.isActive());
        form.setCategoryId(product.getCategory() == null ? null : product.getCategory().getId());
        for (var variant : vendorProductService.variantsOf(product)) {
            VariantForm row = new VariantForm();
            row.setId(variant.getId());
            row.setName(variant.getName());
            row.setExtraPrice(variant.getExtraPrice());
            row.setStock(variant.getStock());
            form.getVariants().add(row);
        }
        if (form.getVariants().isEmpty()) {
            form.getVariants().add(new VariantForm());
        }

        model.addAttribute("form", form);
        model.addAttribute("productId", id);
        model.addAttribute("product", product);
        addCategories(model);
        return "vendor/product-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") ProductForm form,
                         BindingResult bindingResult,
                         @RequestParam(value = "image", required = false) MultipartFile image,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return reRenderForm(model, id);
        }
        try {
            Product product = vendorProductService.update(authentication.getName(), id, form, image);
            redirectAttributes.addFlashAttribute("message", "Da cap nhat san pham: " + product.getName());
        } catch (InvalidImageException ex) {
            model.addAttribute("uploadError", ex.getMessage());
            return reRenderForm(model, id);
        } catch (IllegalStateException ex) {
            model.addAttribute("formError", ex.getMessage());
            return reRenderForm(model, id);
        }
        return "redirect:/vendor/products";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        Product product = vendorProductService.toggleActive(authentication.getName(), id);
        redirectAttributes.addFlashAttribute("message",
                product.isActive() ? "Da bat ban: " + product.getName()
                                   : "Da tat ban: " + product.getName());
        return "redirect:/vendor/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            vendorProductService.delete(authentication.getName(), id);
            redirectAttributes.addFlashAttribute("message", "Da xoa san pham");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("formError", ex.getMessage());
        }
        return "redirect:/vendor/products";
    }

    private String reRenderForm(Model model, Long productId) {
        if (productId != null) {
            model.addAttribute("productId", productId);
        } else {
            model.addAttribute("productId", null);
        }
        addCategories(model);
        return "vendor/product-form";
    }

    private void addCategories(Model model) {
        model.addAttribute("categories", categoryRepository.findByActiveTrue());
    }
}