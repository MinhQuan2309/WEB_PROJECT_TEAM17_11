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
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCart(Cart cart);

    void deleteByCart(Cart cart);

    // ===== Them cho module vendor (Thien) =====

    boolean existsByProduct(Product product);

    boolean existsByVariant(ProductVariant variant);

    /** Topping dang duoc dat trong gio hang nao chua (de chan xoa cung). */
    @Query("select count(ci) from CartItem ci join ci.toppings t where t.id = :toppingId")
    long countCartItemsUsingTopping(@Param("toppingId") Long toppingId);
}
