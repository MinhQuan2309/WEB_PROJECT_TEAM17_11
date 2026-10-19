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
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByProductOrderByIdDesc(Product product, Pageable pageable);

    boolean existsByAuthorAndProduct(User author, Product product);

    @Query("select coalesce(avg(r.rating), 0) from Review r where r.product = :product")
    Double averageRating(@Param("product") Product product);

    long countByProduct(Product product);
}
