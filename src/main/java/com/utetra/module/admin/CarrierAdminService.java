package com.utetra.module.admin;

import com.utetra.entity.Carrier;
import com.utetra.repository.CarrierRepository;
import com.utetra.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * Quan ly nha van chuyen: ten va phi van chuyen.
 * Nha van chuyen da gan vao don hang thi chi tat di, khong xoa.
 */
@Service
public class CarrierAdminService {

    private final CarrierRepository carrierRepository;
    private final OrderRepository orderRepository;

    public CarrierAdminService(CarrierRepository carrierRepository, OrderRepository orderRepository) {
        this.carrierRepository = carrierRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<Carrier> findAll() {
        return carrierRepository.findAllByOrderByIdAsc();
    }

    @Transactional
    public Carrier save(Long id, String name, BigDecimal fee) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalStateException("Ten nha van chuyen khong duoc de trong");
        }
        if (fee == null || fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Phi van chuyen khong duoc am");
        }
        Carrier carrier = (id == null) ? new Carrier() : require(id);
        carrier.setName(name.trim());
        carrier.setFee(fee);
        return carrierRepository.save(carrier);
    }

    @Transactional
    public Carrier toggleActive(Long id) {
        Carrier carrier = require(id);
        carrier.setActive(!carrier.isActive());
        return carrierRepository.save(carrier);
    }

    @Transactional
    public void delete(Long id) {
        Carrier carrier = require(id);
        long used = orderRepository.countByCarrier(carrier);
        if (used > 0) {
            throw new IllegalStateException(
                    "Da co " + used + " don dung nha van chuyen nay, chi tat di chu khong xoa duoc.");
        }
        carrierRepository.delete(carrier);
    }

    private Carrier require(Long id) {
        return carrierRepository.findById(id).orElseThrow(() ->
                new IllegalStateException("Khong tim thay nha van chuyen #" + id));
    }
}
