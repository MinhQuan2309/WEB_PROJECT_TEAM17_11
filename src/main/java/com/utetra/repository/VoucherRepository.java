package com.utetra.repository;

import com.utetra.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCodeAndActiveTrue(String code);

    List<Voucher> findByShopAndActiveTrue(Shop shop);

    /** Voucher cua san: shop = null. */
    List<Voucher> findByShopIsNullAndActiveTrue();

    // ===== Them cho man khuyen mai cua Vendor (Quan) =====

    /** Toan bo ma giam gia cua mot shop, ke ca dang tat. */
    List<Voucher> findByShopOrderByIdDesc(Shop shop);

    boolean existsByCodeIgnoreCase(String code);
}
