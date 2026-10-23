package com.utetra.module.vendor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Bao cao doanh thu cua shop (tuan 5).
 *
 *   GET /vendor/revenue            : xem bao cao theo ngay hoac theo thang
 *   GET /vendor/revenue/export.csv : tai file CSV (mo duoc bang Excel)
 */
@Controller
@RequestMapping("/vendor/revenue")
public class VendorRevenueController {

    private final VendorRevenueService vendorRevenueService;

    public VendorRevenueController(VendorRevenueService vendorRevenueService) {
        this.vendorRevenueService = vendorRevenueService;
    }

    @GetMapping
    public String revenue(@RequestParam(value = "from", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(value = "to", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          @RequestParam(value = "groupBy", required = false) String groupBy,
                          Authentication authentication,
                          Model model) {

        VendorRevenueService.GroupBy kieuGop = parseGroupBy(groupBy);
        VendorRevenueService.RevenueSummary baoCao =
                vendorRevenueService.summary(authentication.getName(), from, to, kieuGop);

        model.addAttribute("report", baoCao);
        model.addAttribute("groupBy", kieuGop.name());
        return "vendor/revenue";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> export(@RequestParam(value = "from", required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(value = "to", required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(value = "groupBy", required = false) String groupBy,
                                         Authentication authentication) {

        VendorRevenueService.RevenueSummary baoCao = vendorRevenueService.summary(
                authentication.getName(), from, to, parseGroupBy(groupBy));

        StringBuilder csv = new StringBuilder();
        // BOM de Excel doc dung tieng Viet.
        csv.append('\uFEFF');
        csv.append("Tu ngay,").append(baoCao.from()).append('\n');
        csv.append("Den ngay,").append(baoCao.to()).append('\n');
        csv.append("Doanh thu,").append(baoCao.revenue()).append('\n');
        csv.append("Hoa hong,").append(baoCao.commission()).append('\n');
        csv.append("Thuc nhan,").append(baoCao.netRevenue()).append('\n');
        csv.append('\n');
        csv.append("Moc thoi gian,Doanh thu,So don da giao\n");
        for (VendorRevenueService.RevenuePoint diem : baoCao.series()) {
            csv.append(diem.label()).append(',').append(diem.revenue()).append(',')
                    .append(diem.orders()).append('\n');
        }

        String tenFile = "doanh-thu-" + baoCao.from() + "-den-" + baoCao.to() + ".csv";
        byte[] noiDung = csv.toString().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + tenFile + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(noiDung);
    }

    private VendorRevenueService.GroupBy parseGroupBy(String groupBy) {
        if (groupBy == null || groupBy.isBlank()) {
            return VendorRevenueService.GroupBy.DAY;
        }
        try {
            return VendorRevenueService.GroupBy.valueOf(groupBy.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return VendorRevenueService.GroupBy.DAY;
        }
    }
}