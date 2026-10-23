package com.utetra.module.vendor;

import com.utetra.entity.Order;
import com.utetra.entity.OrderStatus;
import com.utetra.entity.Shop;
import com.utetra.repository.OrderItemRepository;
import com.utetra.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bao cao doanh thu cua shop (tuan 5).
 *
 * Quy uoc tinh (ghi ro de ca nhom thong nhat):
 *   - Doanh thu = tong tien khach tra (orders.total) cua cac don DA GIAO trong ky.
 *     Don dang giao, don moi, don huy khong tinh vao doanh thu.
 *   - Hoa hong san = doanh thu x commissionRate cua shop (mac dinh 10%).
 *   - Thuc nhan = doanh thu - hoa hong.
 *   - Top mon ban chay: gom theo ten mon trong don, chi tinh don da giao.
 *
 * Bao cao chi doc du lieu cua shop minh (qua requireMyShop) nen khong lo du lieu shop khac.
 */
@Service
public class VendorRevenueService {

    /** Kieu gop nhom cho bang doanh thu. */
    public enum GroupBy {
        DAY, MONTH
    }

    /** Mot dong trong bang doanh thu theo thoi gian. */
    public record RevenuePoint(String label, BigDecimal revenue, long orders) {
    }

    /** Mot dong trong bang mon ban chay. */
    public record TopProduct(String name, long quantity, BigDecimal revenue) {
    }

    /** Toan bo so lieu cua trang bao cao. */
    public record RevenueSummary(LocalDate from,
                                 LocalDate to,
                                 GroupBy groupBy,
                                 BigDecimal revenue,
                                 BigDecimal commission,
                                 BigDecimal netRevenue,
                                 BigDecimal averageOrderValue,
                                 long orderCount,
                                 long deliveredCount,
                                 long cancelledCount,
                                 List<TopProduct> topProducts,
                                 List<RevenuePoint> series) {
    }

    private static final int MAX_TOP_PRODUCTS = 10;
    private static final DateTimeFormatter LABEL_DAY = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter LABEL_MONTH = DateTimeFormatter.ofPattern("MM/yyyy");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final VendorShopService vendorShopService;

    public VendorRevenueService(OrderRepository orderRepository,
                                OrderItemRepository orderItemRepository,
                                VendorShopService vendorShopService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.vendorShopService = vendorShopService;
    }

    @Transactional(readOnly = true)
    public RevenueSummary summary(String username, LocalDate from, LocalDate to, GroupBy groupBy) {
        Shop shop = vendorShopService.requireMyShop(username);
        return summaryOfShop(shop, from, to, groupBy);
    }

    @Transactional(readOnly = true)
    public RevenueSummary summaryOfShop(Shop shop, LocalDate from, LocalDate to, GroupBy groupBy) {
        LocalDate ngayKetThuc = to == null ? LocalDate.now() : to;
        LocalDate ngayBatDau = from == null ? ngayKetThuc.withDayOfMonth(1) : from;
        if (ngayBatDau.isAfter(ngayKetThuc)) {
            LocalDate tam = ngayBatDau;
            ngayBatDau = ngayKetThuc;
            ngayKetThuc = tam;
        }
        if (ngayBatDau.isBefore(ngayKetThuc.minusDays(365))) {
            ngayBatDau = ngayKetThuc.minusDays(365);
        }
        GroupBy kieuGop = groupBy == null ? GroupBy.DAY : groupBy;

        LocalDateTime tu = ngayBatDau.atStartOfDay();
        LocalDateTime den = ngayKetThuc.plusDays(1).atStartOfDay();
        List<Order> donTrongKy = orderRepository.ordersOfShopBetween(shop, tu, den);

        BigDecimal doanhThu = BigDecimal.ZERO;
        long soDonDaGiao = 0;
        long soDonHuy = 0;
        Map<String, BigDecimal> doanhThuTheoMoc = new LinkedHashMap<>();
        Map<String, Long> soDonTheoMoc = new LinkedHashMap<>();

        for (Order order : donTrongKy) {
            if (order.getStatus() == OrderStatus.DELIVERED) {
                BigDecimal tien = order.getTotal() == null ? BigDecimal.ZERO : order.getTotal();
                doanhThu = doanhThu.add(tien);
                soDonDaGiao++;
                String moc = mocThoiGian(order.getCreatedAt(), kieuGop);
                doanhThuTheoMoc.merge(moc, tien, BigDecimal::add);
                soDonTheoMoc.merge(moc, 1L, Long::sum);
            } else if (laDonHuy(order)) {
                soDonHuy++;
            }
        }

        BigDecimal tyLeHoaHong = shop.getCommissionRate() == null
                ? BigDecimal.ZERO : shop.getCommissionRate();
        BigDecimal hoaHong = doanhThu.multiply(tyLeHoaHong)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal thucNhan = doanhThu.subtract(hoaHong);
        BigDecimal trungBinh = soDonDaGiao == 0
                ? BigDecimal.ZERO
                : doanhThu.divide(BigDecimal.valueOf(soDonDaGiao), 0, RoundingMode.HALF_UP);

        return new RevenueSummary(
                ngayBatDau,
                ngayKetThuc,
                kieuGop,
                doanhThu,
                hoaHong,
                thucNhan,
                trungBinh,
                donTrongKy.size(),
                soDonDaGiao,
                soDonHuy,
                topProducts(shop, tu, den),
                duongDoanhThu(ngayBatDau, ngayKetThuc, kieuGop, doanhThuTheoMoc, soDonTheoMoc));
    }

    private boolean laDonHuy(Order order) {
        return order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.RETURNED_REFUNDED;
    }

    private List<TopProduct> topProducts(Shop shop, LocalDateTime tu, LocalDateTime den) {
        List<Object[]> rows = orderItemRepository.topProductsOfShop(shop, OrderStatus.DELIVERED, tu, den);
        List<TopProduct> ketQua = new ArrayList<>();
        for (Object[] row : rows) {
            if (ketQua.size() >= MAX_TOP_PRODUCTS) {
                break;
            }
            ketQua.add(new TopProduct(
                    String.valueOf(row[0]),
                    row[1] == null ? 0L : ((Number) row[1]).longValue(),
                    row[2] == null ? BigDecimal.ZERO : (BigDecimal) row[2]));
        }
        return ketQua;
    }

    /** Dien day du cac moc thoi gian trong ky, moc nao khong co don thi ghi 0. */
    private List<RevenuePoint> duongDoanhThu(LocalDate tu, LocalDate den, GroupBy kieuGop,
                                             Map<String, BigDecimal> doanhThuTheoMoc,
                                             Map<String, Long> soDonTheoMoc) {
        List<RevenuePoint> diem = new ArrayList<>();
        if (kieuGop == GroupBy.MONTH) {
            YearMonth moc = YearMonth.from(tu);
            YearMonth cuoi = YearMonth.from(den);
            while (!moc.isAfter(cuoi)) {
                String nhan = moc.format(LABEL_MONTH);
                diem.add(new RevenuePoint(nhan,
                        doanhThuTheoMoc.getOrDefault(nhan, BigDecimal.ZERO),
                        soDonTheoMoc.getOrDefault(nhan, 0L)));
                moc = moc.plusMonths(1);
            }
            return diem;
        }
        LocalDate ngay = tu;
        while (!ngay.isAfter(den)) {
            String nhan = ngay.format(LABEL_DAY);
            diem.add(new RevenuePoint(nhan,
                    doanhThuTheoMoc.getOrDefault(nhan, BigDecimal.ZERO),
                    soDonTheoMoc.getOrDefault(nhan, 0L)));
            ngay = ngay.plusDays(1);
        }
        return diem;
    }

    private String mocThoiGian(LocalDateTime thoiDiem, GroupBy kieuGop) {
        LocalDate ngay = thoiDiem == null ? LocalDate.now() : thoiDiem.toLocalDate();
        return kieuGop == GroupBy.MONTH ? ngay.format(LABEL_MONTH) : ngay.format(LABEL_DAY);
    }
}