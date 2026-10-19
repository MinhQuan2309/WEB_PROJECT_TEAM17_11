package com.utetra.repository;

import com.utetra.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Ke thua JpaSpecificationExecutor de ProductSearchService ghep dieu kien loc dong.
 * Cac ham findTop... phuc vu 4 block san pham o trang chu.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long>,
                                          JpaSpecificationExecutor<Product> {

    Page<Product> findByActiveTrue(Pageable pageable);

    Page<Product> findByShopAndActiveTrue(Shop shop, Pageable pageable);

    Page<Product> findByCategoryAndActiveTrue(Category category, Pageable pageable);

    Page<Product> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<Product> findByActiveTrueOrderBySoldCountDesc(Pageable pageable);

    Page<Product> findByActiveTrueOrderByRatingAvgDescRatingCountDesc(Pageable pageable);

    Page<Product> findByActiveTrueOrderByLikeCountDesc(Pageable pageable);

    /**
     * Nap kem shop va category trong cung mot truy van de trang chu khong phai
     * ban them N cau SELECT (tranh N+1) va khong dinh LazyInitializationException.
     */
    @EntityGraph(attributePaths = {"shop", "category"})
    List<Product> findTop10ByActiveTrueOrderBySoldCountDesc();

    // ===== Them cho module vendor (Thien) =====

    /** Danh sach san pham cua shop, ke ca san pham dang an (man hinh quan ly cua Vendor). */
    @EntityGraph(attributePaths = {"category"})
    Page<Product> findByShopOrderByIdDesc(Shop shop, Pageable pageable);

    long countByShop(Shop shop);

    long countByShopAndActiveTrue(Shop shop);
}
