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
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder(Order order);

    // ===== Them cho module vendor (Thien) =====

    /** San pham da tung nam trong don hang thi khong cho xoa cung. */
    boolean existsByProduct(Product product);

    /**
     * Top mon ban chay cua shop trong khoang thoi gian, chi tinh don da giao.
     * Tra ve: [ten mon, tong so luong, tong tien].
     */
    @Query("""
            select i.productName, sum(i.quantity), sum(i.lineTotal)
            from OrderItem i
            where i.order.shop = :shop and i.order.status = :status
              and i.order.createdAt >= :from and i.order.createdAt < :to
            group by i.productName
            order by sum(i.quantity) desc
            """)
    List<Object[]> topProductsOfShop(@Param("shop") Shop shop,
                                     @Param("status") OrderStatus status,
                                     @Param("from") java.time.LocalDateTime from,
                                     @Param("to") java.time.LocalDateTime to);
}
