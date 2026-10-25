package com.utetra.repository;

import com.utetra.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    List<Promotion> findByShopAndActiveTrue(Shop shop);

    /** Khuyen mai cua san: shop = null. */
    List<Promotion> findByShopIsNullAndActiveTrue();

    /** Toan bo khuyen mai cua san, ke ca dang tat (man quan tri). */
    List<Promotion> findByShopIsNullOrderByIdDesc();

    // ===== Them cho man khuyen mai cua Vendor (Quan) =====

    /** Toan bo khuyen mai cua mot shop, ke ca dang tat. */
    List<Promotion> findByShopOrderByIdDesc(Shop shop);

    List<Promotion> findByProductAndActiveTrue(Product product);

    /** Cac chuong trinh dang chay tai thoi diem now. */
    @Query("""
            select p from Promotion p
            where p.active = true and p.startAt <= :now and p.endAt >= :now
              and (:shop is null or p.shop = :shop or p.shop is null)
            """)
    List<Promotion> findRunning(@Param("now") LocalDateTime now, @Param("shop") Shop shop);
}
