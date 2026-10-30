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

    // ===== Them cho man quan tri nha van chuyen (Quan) =====

    /** Nha van chuyen da duoc dung trong don thi khong cho xoa. */
    long countByCarrier(Carrier carrier);

    // ===== Them cho khu Manager (Quan) =====

    long countByShopIn(List<Shop> shops);

    @Query("""
            select coalesce(sum(o.total), 0) from Order o
            where o.shop in :shops and o.status = com.utetra.entity.OrderStatus.DELIVERED
            """)
    BigDecimal revenueOfShops(@Param("shops") List<Shop> shops);

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

    /** Don cua shop trong khoang thoi gian, dung cho bao cao doanh thu (tuan 5). */
    @Query("""
            select o from Order o
            where o.shop = :shop and o.createdAt >= :from and o.createdAt < :to
            order by o.id asc
            """)
    List<Order> ordersOfShopBetween(@Param("shop") Shop shop,
                                    @Param("from") java.time.LocalDateTime from,
                                    @Param("to") java.time.LocalDateTime to);

    /** Don dang cho shipper nhan: da xac nhan va chua co ai nhan (tuan 5). */
    Page<Order> findByStatusAndShipperIsNullOrderByIdAsc(OrderStatus status, Pageable pageable);

    /** Thong ke cua shipper theo trang thai (tuan 5). */
    long countByShipperAndStatus(User shipper, OrderStatus status);

    /** Don dang do dang cua mot shipper, dung khi go vai shipper. */
    long countByShipperAndStatusIn(User shipper, java.util.Collection<OrderStatus> statuses);

    /** Don cua shipper trong khoang thoi gian, dung cho thong ke COD (tuan 5). */
    @Query("""
            select o from Order o
            where o.shipper = :shipper and o.createdAt >= :from and o.createdAt < :to
            order by o.id asc
            """)
    List<Order> ordersOfShipperBetween(@Param("shipper") User shipper,
                                       @Param("from") java.time.LocalDateTime from,
                                       @Param("to") java.time.LocalDateTime to);
}
