package com.utetra.module.catalog;

import com.utetra.entity.Product;
import com.utetra.repository.CategoryRepository;
import com.utetra.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * KHUNG CHO HAI. Hien tai chi lam dung phan Guest o tuan 1:
 * trang chu hien tren 10 san pham cua cac shop, sap xep giam dan theo luot ban.
 *
 * Hai lam tiep tai module nay:
 *   - trang chu cho User: 4 block 20 san pham (moi / ban chay / danh gia / yeu thich)
 *     co phan trang hoac lazy loading
 *   - trang san pham theo danh muc, co loc va sap xep (dung ProductSearchService)
 *   - trang chi tiet san pham
 */
@Controller
public class HomeController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public HomeController(ProductRepository productRepository,
                          CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        List<Product> bestSellers = productRepository.findTop10ByActiveTrueOrderBySoldCountDesc();
        model.addAttribute("products", bestSellers);
        model.addAttribute("categories", categoryRepository.findByParentIsNullAndActiveTrue());
        return "home/index";
    }
}
