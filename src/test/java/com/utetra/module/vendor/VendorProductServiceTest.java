package com.utetra.module.vendor;

import com.utetra.entity.Category;
import com.utetra.entity.Order;
import com.utetra.entity.OrderItem;
import com.utetra.entity.Product;
import com.utetra.entity.ProductVariant;
import com.utetra.entity.Shop;
import com.utetra.module.vendor.dto.ProductForm;
import com.utetra.module.vendor.dto.VariantForm;
import com.utetra.repository.CategoryRepository;
import com.utetra.repository.OrderItemRepository;
import com.utetra.repository.OrderRepository;
import com.utetra.repository.ProductRepository;
import com.utetra.repository.ProductVariantRepository;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem tra nghiep vu san pham cua tuan 2:
 * CRUD + size, chan IDOR, chan xoa san pham da ban, va khong nhan danh muc khong hop le.
 */
@SpringBootTest
@Transactional
class VendorProductServiceTest {

    @Autowired VendorProductService productService;
    @Autowired ProductRepository productRepository;
    @Autowired ProductVariantRepository variantRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ShopRepository shopRepository;
    @Autowired UserRepository userRepository;

    private Shop shopOf(String username) {
        return shopRepository.findByOwner(userRepository.findByUsername(username).orElseThrow())
                .orElseThrow();
    }

    private Product sanPhamDauTienCua(String username) {
        return productRepository
                .findByShopOrderByIdDesc(shopOf(username), PageRequest.of(0, 1))
                .getContent().get(0);
    }

    private ProductForm form(String name, long basePrice, String... sizes) {
        ProductForm form = new ProductForm();
        form.setName(name);
        form.setBasePrice(BigDecimal.valueOf(basePrice));
        form.setDescription("Mo ta " + name);
        form.setActive(true);
        for (int i = 0; i < sizes.length; i++) {
            VariantForm row = new VariantForm();
            row.setName(sizes[i]);
            row.setExtraPrice(BigDecimal.valueOf(i * 5000L));
            row.setStock(10 + i);
            form.getVariants().add(row);
        }
        return form;
    }

    @Test
    @DisplayName("Tao san pham: thuoc dung shop dang nhap va luu du cac size")
    void taoSanPhamThuocDungShopVaLuuSize() {
        Product product = productService.create("vendor1",
                form("Tra sua test tao moi", 30000, "Size S", "Size M"), null);

        assertNotNull(product.getId());
        assertEquals(shopOf("vendor1").getId(), product.getShop().getId(),
                "San pham phai thuoc shop cua nguoi dang nhap, khong lay shop tu form");
        assertTrue(product.isActive());

        List<ProductVariant> variants = variantRepository.findByProductOrderByIdAsc(product);
        assertEquals(2, variants.size());
        assertEquals("Size S", variants.get(0).getName());
        assertEquals("Size M", variants.get(1).getName());
        assertEquals(0, variants.get(1).getExtraPrice().compareTo(BigDecimal.valueOf(5000)));
    }

    @Test
    @DisplayName("IDOR: Vendor khac khong sua / xoa / tat ban duoc san pham cua minh")
    void vendorKhacKhongChamDuocSanPham() {
        Product cua = sanPhamDauTienCua("vendor1");
        String tenCu = cua.getName();

        assertThrows(AccessDeniedException.class, () -> productService.requireOwnedProduct("vendor2", cua.getId()));
        assertThrows(AccessDeniedException.class, () -> productService.update(
                "vendor2", cua.getId(), form("Ten bi chiem", 1000, "Size X"), null));
        assertThrows(AccessDeniedException.class, () -> productService.toggleActive("vendor2", cua.getId()));
        assertThrows(AccessDeniedException.class, () -> productService.delete("vendor2", cua.getId()));

        Product sau = productRepository.findById(cua.getId()).orElseThrow();
        assertEquals(tenCu, sau.getName(), "Du lieu san pham khong duoc doi");
        assertEquals(3, variantRepository.findByProductOrderByIdAsc(sau).size(),
                "Size cua san pham khong bi thay doi");
    }

    @Test
    @DisplayName("Sua san pham: dong size it hon thi xoa size thua, nhieu hon thi them size moi")
    void dongBoSizeKhiSua() {
        Product product = sanPhamDauTienCua("vendor1");
        assertEquals(3, variantRepository.findByProductOrderByIdAsc(product).size(),
                "San pham mau co 3 size");

        productService.update("vendor1", product.getId(),
                form("Tra sua rut size", 35000, "Size S", "Size M"), null);
        List<ProductVariant> sau = variantRepository.findByProductOrderByIdAsc(product);
        assertEquals(2, sau.size(), "Bo 1 dong size thi phai xoa size thua");
        assertEquals("Size S", sau.get(0).getName());

        productService.update("vendor1", product.getId(),
                form("Tra sua them size", 35000, "Size S", "Size M", "Size L", "Size XL"), null);
        List<ProductVariant> sauNua = variantRepository.findByProductOrderByIdAsc(product);
        assertEquals(4, sauNua.size(), "Them dong size thi phai tao size moi");
        assertEquals("Size XL", sauNua.get(3).getName());
    }

    @Test
    @DisplayName("Dong size bo trong ten thi bi bo qua, khong tao size rac")
    void dongSizeTrongBiBoQua() {
        ProductForm form = form("Tra sua size rong", 25000, "Size S");
        VariantForm trong = new VariantForm();
        trong.setName("   ");
        form.getVariants().add(trong);

        Product product = productService.create("vendor1", form, null);
        assertEquals(1, variantRepository.findByProductOrderByIdAsc(product).size());
    }

    @Test
    @DisplayName("Xoa san pham chua bi tham chieu thi xoa duoc ca size")
    void xoaSanPhamChuaThamChieu() {
        Product product = productService.create("vendor1", form("San pham de xoa", 20000, "Size S"), null);
        Long productId = product.getId();

        productService.delete("vendor1", productId);

        assertFalse(productRepository.findById(productId).isPresent(), "San pham phai bi xoa");
        assertTrue(variantRepository.findByProductOrderByIdAsc(product).isEmpty(), "Size phai bi xoa theo");
    }

    @Test
    @DisplayName("San pham da nam trong don hang thi khong xoa cung duoc, chi duoc tat ban")
    void sanPhamDaBanThiKhongXoaDuoc() {
        Product product = sanPhamDauTienCua("vendor1");

        Order order = new Order();
        order.setCode("TEST-" + System.nanoTime());
        order.setCustomer(userRepository.findByUsername("khach").orElseThrow());
        order.setShop(product.getShop());
        order.setReceiverName("Khach Test");
        order.setReceiverPhone("0900000000");
        order.setShippingAddress("1 Duong Test, Thu Duc");
        orderRepository.save(order);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setProductName(product.getName());
        item.setUnitPrice(product.getBasePrice());
        item.setQuantity(1);
        item.setLineTotal(product.getBasePrice());
        orderItemRepository.save(item);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> productService.delete("vendor1", product.getId()));
        assertTrue(ex.getMessage().contains("Tat ban"), "Thong bao phai huong dan dung chuc nang Tat ban");

        productService.toggleActive("vendor1", product.getId());
        assertFalse(productRepository.findById(product.getId()).orElseThrow().isActive());
    }

    @Test
    @DisplayName("Chi nhan danh muc dang hoat dong, id la hoac danh muc da an thi bo qua")
    void chiNhanDanhMucDangHoatDong() {
        Category danhMuc = categoryRepository.findByActiveTrue().get(0);

        ProductForm coDanhMuc = form("San pham co danh muc", 30000, "Size S");
        coDanhMuc.setCategoryId(danhMuc.getId());
        Product product = productService.create("vendor1", coDanhMuc, null);
        assertEquals(danhMuc.getId(), product.getCategory().getId());

        ProductForm idLa = form("San pham id danh muc la", 30000, "Size S");
        idLa.setCategoryId(999_999L);
        assertNull(productService.create("vendor1", idLa, null).getCategory());

        danhMuc.setActive(false);
        categoryRepository.save(danhMuc);
        ProductForm danhMucDaAn = form("San pham danh muc da an", 30000, "Size S");
        danhMucDaAn.setCategoryId(danhMuc.getId());
        assertNull(productService.create("vendor1", danhMucDaAn, null).getCategory(),
                "Danh muc da an thi khong duoc gan vao san pham");
    }

    @Test
    @DisplayName("Danh sach san pham chi tra ve san pham cua shop minh")
    void danhSachChiCoSanPhamCuaMinh() {
        var cua = productService.listMyProducts("vendor1", null, 0, 50);
        long tongCua1 = cua.getTotalElements();
        assertTrue(tongCua1 > 0);
        assertTrue(cua.getContent().stream()
                        .allMatch(p -> p.getShop().getId().equals(shopOf("vendor1").getId())),
                "Khong duoc lan sang san pham cua shop khac");

        productService.create("vendor1", form("San pham an thu", 10000, "Size S"), null);
        long tongSau = productService.listMyProducts("vendor1", true, 0, 50).getTotalElements();
        long tongAn = productService.listMyProducts("vendor1", false, 0, 50).getTotalElements();
        assertEquals(tongCua1 + 1, tongSau + tongAn, "Loc theo trang thai ban phai khop tong so");
    }
}