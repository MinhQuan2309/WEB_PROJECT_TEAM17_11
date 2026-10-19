package com.utetra.module.orderstatus;

import com.utetra.entity.OrderStatus;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Quy tac chuyen trang thai don hang, dung chung cho ca 4 vai (Thien viet).
 *
 * Vi sao tach ra mot lop rieng: man hinh Vendor (tuan 3), man hinh Shipper (tuan 5)
 * va nut huy don cua khach (Hai) deu phai ap dung dung mot bo quy tac. Neu moi noi
 * tu viet if/else thi chi can mot cho quen kiem tra la don hang nhay bua.
 *
 * Luong chuan cua de tai:
 *   NEW -> CONFIRMED -> PICKED_UP -> SHIPPING -> DELIVERED
 *   va cac nhanh dung giua dong: CANCELLED, RETURNED_REFUNDED
 *
 * Nguyen tac: CANCELLED va RETURNED_REFUNDED la trang thai ket thuc, khong di tiep.
 */
public final class OrderStatusTransitions {

    /** Do thi chuyen trang thai hop le ve mat nghiep vu (chua xet ai duoc lam). */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED;

    /** Trang thai ma tung vai duoc phep chuyen toi. */
    private static final Map<OrderActor, Map<OrderStatus, Set<OrderStatus>>> ALLOWED_BY_ACTOR;

    static {
        Map<OrderStatus, Set<OrderStatus>> allowed = new EnumMap<>(OrderStatus.class);
        allowed.put(OrderStatus.NEW, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        allowed.put(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PICKED_UP, OrderStatus.CANCELLED));
        allowed.put(OrderStatus.PICKED_UP, EnumSet.of(OrderStatus.SHIPPING));
        allowed.put(OrderStatus.SHIPPING, EnumSet.of(OrderStatus.DELIVERED, OrderStatus.RETURNED_REFUNDED));
        allowed.put(OrderStatus.DELIVERED, EnumSet.of(OrderStatus.RETURNED_REFUNDED));
        allowed.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        allowed.put(OrderStatus.RETURNED_REFUNDED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED = Collections.unmodifiableMap(allowed);

        Map<OrderActor, Map<OrderStatus, Set<OrderStatus>>> byActor = new EnumMap<>(OrderActor.class);
        byActor.put(OrderActor.VENDOR, Map.of(
                OrderStatus.NEW, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
                OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PICKED_UP, OrderStatus.CANCELLED),
                OrderStatus.SHIPPING, EnumSet.of(OrderStatus.RETURNED_REFUNDED),
                OrderStatus.DELIVERED, EnumSet.of(OrderStatus.RETURNED_REFUNDED)));
        byActor.put(OrderActor.SHIPPER, Map.of(
                OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PICKED_UP),
                OrderStatus.PICKED_UP, EnumSet.of(OrderStatus.SHIPPING),
                OrderStatus.SHIPPING, EnumSet.of(OrderStatus.DELIVERED, OrderStatus.RETURNED_REFUNDED)));
        byActor.put(OrderActor.CUSTOMER, Map.of(
                OrderStatus.NEW, EnumSet.of(OrderStatus.CANCELLED)));
        byActor.put(OrderActor.ADMIN, allowed);
        ALLOWED_BY_ACTOR = Collections.unmodifiableMap(byActor);
    }

    private OrderStatusTransitions() {
    }

    /** Chuyen trang thai nay co hop le ve nghiep vu khong (khong xet vai). */
    public static boolean canMove(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    /** Vai nay co duoc phep chuyen tu from sang to khong. */
    public static boolean canMove(OrderActor actor, OrderStatus from, OrderStatus to) {
        if (actor == null || !canMove(from, to)) {
            return false;
        }
        return ALLOWED_BY_ACTOR.getOrDefault(actor, Map.of())
                .getOrDefault(from, Set.of())
                .contains(to);
    }

    /** Tat ca trang thai co the chuyen toi tu mot trang thai (moi vai). */
    public static Set<OrderStatus> nextStates(OrderStatus from) {
        return Collections.unmodifiableSet(ALLOWED.getOrDefault(from, Set.of()));
    }

    /** Cac trang thai ma mot vai cu the duoc phep chuyen toi. */
    public static Set<OrderStatus> nextStates(OrderActor actor, OrderStatus from) {
        return Collections.unmodifiableSet(ALLOWED_BY_ACTOR.getOrDefault(actor, Map.of())
                .getOrDefault(from, Set.of()));
    }

    /** Trang thai ket thuc: huy hoac tra hang - hoan tien. */
    public static boolean isTerminal(OrderStatus status) {
        return status == OrderStatus.CANCELLED || status == OrderStatus.RETURNED_REFUNDED;
    }
}