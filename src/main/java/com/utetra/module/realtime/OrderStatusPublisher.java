package com.utetra.module.realtime;

import com.utetra.entity.Order;

/**
 * Cong day thong bao don hang ra ben ngoai (tuan 4).
 *
 * Vi sao phai co interface: module orderstatus (doi trang thai don) chi nen biet
 * "co nguoi can nghe thay doi nay", khong can biet ben duoi la STOMP hay gi khac.
 * Khi viet test cung de thay the bang ban gia de kiem tra da goi dung luc chua.
 */
public interface OrderStatusPublisher {

    /**
     * Day trang thai moi cua don cho khach dat don, shop va shipper duoc phan cong.
     *
     * @param order   don vua doi trang thai (da duoc luu)
     * @param message loi nhan ngan hien cho nguoi dung, vi du "Don da duoc xac nhan"
     */
    void publishOrderStatus(Order order, String message);

    /**
     * Bao cho shop biet vua co don moi (man hinh Vendor hien thong bao ngay).
     * Ham nay duoc goi tu luong dat hang cua Hai sau khi luu don.
     */
    void publishNewOrderToShop(Order order);
}