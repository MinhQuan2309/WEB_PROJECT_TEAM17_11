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
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByCode(String code);

    /** Lich su mua hang cua khach, loc theo trang thai. */
    Page<Order> findByCustomerOrderByIdDesc(User customer, Pageable pageable);

    Page<Order> findByCustomerAndStatusOrderByIdDesc(User customer, OrderStatus status, Pageable pageable);

    /** Don hang cua shop (man hinh Vendor). */
    Page<Order> findByShopOrderByIdDesc(Shop shop, Pageable pageable);

    Page<Order> findByShopAndStatusOrderByIdDesc(Shop shop, OrderStatus status, Pageable pageable);

    /** Don duoc phan cong cho shipper. */
    Page<Order> findByShipperOrderByIdDesc(User shipper, Pageable pageable);

    Page<Order> findByShipperAndStatusOrderByIdDesc(User shipper, OrderStatus status, Pageable pageable);

    long countByShopAndStatus(Shop shop, OrderStatus status);

    // ===== Them cho module vendor / shipper (Thien) =====

    long countByShop(Shop shop);

    /** Doanh thu: chi tinh don da giao. */
    @Query("""
            select coalesce(sum(o.total), 0) from Order o
            where o.shop = :shop and o.status = com.utetra.entity.OrderStatus.DELIVERED
            """)
    BigDecimal revenueOfShop(@Param("shop") Shop shop);

    @Query("""
            select coalesce(sum(o.total), 0) from Order o
            where o.status = com.utetra.entity.OrderStatus.DELIVERED
            """)
    BigDecimal revenueOfPlatform();
}
