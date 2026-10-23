package com.utetra.module.payment;

import com.utetra.entity.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Tim dung Strategy theo phuong thuc thanh toan cua don (tuan 4).
 *
 * Spring tu tiem tat ca bean implements PaymentStrategy vao danh sach, nen them
 * cong thanh toan moi chi can them mot @Component - khong phai sua lop nay.
 */
@Component
public class PaymentStrategyRegistry {

    private final Map<PaymentMethod, PaymentStrategy> strategies;

    public PaymentStrategyRegistry(List<PaymentStrategy> danhSach) {
        Map<PaymentMethod, PaymentStrategy> map = new EnumMap<>(PaymentMethod.class);
        for (PaymentStrategy strategy : danhSach) {
            map.put(strategy.method(), strategy);
        }
        this.strategies = Map.copyOf(map);
    }

    /** Phuong thuc null hoac khong ro duoc coi la COD cho an toan. */
    public PaymentStrategy of(PaymentMethod method) {
        PaymentMethod key = method == null ? PaymentMethod.COD : method;
        PaymentStrategy strategy = strategies.get(key);
        if (strategy == null) {
            throw new IllegalStateException("Chua ho tro phuong thuc thanh toan " + key);
        }
        return strategy;
    }

    public List<PaymentStrategy> all() {
        return List.copyOf(strategies.values());
    }
}