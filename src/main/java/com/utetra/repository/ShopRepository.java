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
public interface ShopRepository extends JpaRepository<Shop, Long> {

    Optional<Shop> findByOwner(User owner);

    List<Shop> findByStatus(ShopStatus status);

    /** Pham vi quan ly cua mot Manager. */
    List<Shop> findByManager(User manager);

    Page<Shop> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
