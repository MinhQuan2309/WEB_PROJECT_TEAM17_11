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
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByOrder(Order order);

    List<Shipment> findByShipper(User shipper);

    /** Don shipper dang cam, chua giao xong. */
    List<Shipment> findByShipperAndDeliveredAtIsNull(User shipper);

    long countByShipperAndDeliveredAtIsNotNull(User shipper);

    long countByShipperAndFailedReasonIsNotNull(User shipper);
}
