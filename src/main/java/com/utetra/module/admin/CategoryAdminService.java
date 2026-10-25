package com.utetra.module.admin;

import com.utetra.common.util.SlugUtils;
import com.utetra.entity.Category;
import com.utetra.repository.CategoryRepository;
import com.utetra.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Quan ly danh muc san pham, co cap cha - con mot tang.
 *
 * Slug sinh tu dat ten, trung thi them hau to so. Danh muc dang co san pham
 * hoac dang co danh muc con thi khong cho xoa, de khong lam hong du lieu cu.
 */
@Service
public class CategoryAdminService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryAdminService(CategoryRepository categoryRepository,
                                ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public List<Category> findRoots() {
        return categoryRepository.findByParentIsNullOrderByIdAsc();
    }

    @Transactional
    public Category save(Long id, String name, Long parentId, String imageUrl) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalStateException("Ten danh muc khong duoc de trong");
        }
        Category category = (id == null) ? new Category() : require(id);
        category.setName(name.trim());
        category.setImageUrl(StringUtils.hasText(imageUrl) ? imageUrl.trim() : null);
        category.setParent(resolveParent(category, parentId));

        if (!StringUtils.hasText(category.getSlug()) || id == null) {
            category.setSlug(uniqueSlug(name));
        }
        return categoryRepository.save(category);
    }

    @Transactional
    public Category toggleActive(Long id) {
        Category category = require(id);
        category.setActive(!category.isActive());
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = require(id);
        long products = productRepository.countByCategory(category);
        if (products > 0) {
            throw new IllegalStateException(
                    "Danh muc dang co " + products + " san pham, khong xoa duoc. Hay tat danh muc thay vi xoa.");
        }
        long children = categoryRepository.countByParent(category);
        if (children > 0) {
            throw new IllegalStateException(
                    "Danh muc dang co " + children + " danh muc con, hay xoa danh muc con truoc.");
        }
        categoryRepository.delete(category);
    }

    private Category resolveParent(Category category, Long parentId) {
        if (parentId == null) {
            return null;
        }
        Category parent = require(parentId);
        if (category.getId() != null && parent.getId().equals(category.getId())) {
            throw new IllegalStateException("Danh muc khong the la cha cua chinh no");
        }
        if (parent.getParent() != null) {
            throw new IllegalStateException("Chi ho tro danh muc hai cap, khong chon danh muc con lam cha");
        }
        return parent;
    }

    private String uniqueSlug(String name) {
        String base = SlugUtils.toSlug(name);
        if (base.isEmpty()) {
            base = "danh-muc";
        }
        String slug = base;
        int i = 2;
        while (categoryRepository.existsBySlug(slug)) {
            slug = base + "-" + i++;
        }
        return slug;
    }

    private Category require(Long id) {
        return categoryRepository.findById(id).orElseThrow(() ->
                new IllegalStateException("Khong tim thay danh muc #" + id));
    }
}
