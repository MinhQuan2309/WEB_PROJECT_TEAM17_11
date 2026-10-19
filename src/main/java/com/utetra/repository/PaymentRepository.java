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
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrder(Order order);

    Optional<Payment> findByTransactionId(String transactionId);

    /** Lan thanh toan gan nhat cua don, dung khi xu ly callback cua cong thanh toan. */
    Optional<Payment> findTopByOrderOrderByIdDesc(Order order);

    List<Payment> findByStatus(PaymentStatus status);
}
