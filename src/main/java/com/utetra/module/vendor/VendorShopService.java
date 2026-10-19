package com.utetra.module.vendor;

import com.utetra.entity.Role;
import com.utetra.entity.Shop;
import com.utetra.entity.ShopStatus;
import com.utetra.entity.User;
import com.utetra.module.media.CloudinaryService;
import com.utetra.module.vendor.dto.ShopRegistrationRequest;
import com.utetra.module.vendor.dto.ShopUpdateRequest;
import com.utetra.module.vendor.upload.ImageUploadValidator;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Nghiep vu shop cho Vendor (tuan 1: dang ky shop + quan ly thong tin shop).
 *
 * Nguyen tac an toan ap dung o day, va se lap lai cho san pham / don hang / khuyen mai:
 *
 *   1. MOI thao tac sua du lieu deu di qua `requireOwnedShop` hoac `requireMyShop`
 *      de kiem tra chu so huu truoc khi ghi (chong IDOR: Vendor A khong duoc sua
 *      shop cua Vendor B du co doan dung id tren URL).
 *   2. DTO chi chua truong duoc phep sua, nen nguoi dung khong the tu gan
 *      status = ACTIVE hay commissionRate = 0 tu form (chong mass assignment).
 *   3. Tai khoan dang ky shop VAN LA USER. Chi khi Admin/Manager duyet
 *      (doi ShopStatus sang ACTIVE) thi moi nang role len VENDOR.
 *      -> xem hop dong nay o docs/BaoCao/BaoCao_Thien.md
 */
@Service
public class VendorShopService {

    /** Thu muc tren Cloudinary cho anh cua shop. */
    public static final String SHOP_IMAGE_FOLDER = "shops";

    /** Chiet khau san mac dinh cho shop moi; Admin/Manager chinh lai sau. */
    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("10.00");

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final ImageUploadValidator imageUploadValidator;

    public VendorShopService(ShopRepository shopRepository,
                             UserRepository userRepository,
                             CloudinaryService cloudinaryService,
                             ImageUploadValidator imageUploadValidator) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
        this.imageUploadValidator = imageUploadValidator;
    }

    /** Shop cua nguoi dang dang nhap, neu da co. */
    @Transactional(readOnly = true)
    public Optional<Shop> findMyShop(String username) {
        return userRepository.findByUsername(username)
                .flatMap(shopRepository::findByOwner);
    }

    /**
     * Shop cua chinh nguoi dang dang nhap.
     *
     * @throws AccessDeniedException neu tai khoan chua co shop (khong duoc vao khu vendor).
     */
    @Transactional(readOnly = true)
    public Shop requireMyShop(String username) {
        return findMyShop(username).orElseThrow(() ->
                new AccessDeniedException("Tai khoan " + username + " chua co shop nao"));
    }

    /**
     * Diem kiem tra chu so huu dung chung cho ca module vendor.
     *
     * @throws org.springframework.web.server.ResponseStatusException 404 neu shop khong ton tai
     * @throws AccessDeniedException 403 neu shop thuoc ve nguoi khac
     */
    @Transactional(readOnly = true)
    public Shop requireOwnedShop(String username, Long shopId) {
        Shop shop = shopRepository.findById(shopId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay shop #" + shopId));
        if (shop.getOwner() == null || !shop.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("Shop #" + shopId + " khong thuoc tai khoan " + username);
        }
        return shop;
    }

    /**
     * Dang ky shop moi. Shop luon duoc tao o trang thai PENDING va role cua chu shop
     * khong doi (van la USER) cho toi khi Admin/Manager duyet.
     */
    @Transactional
    public Shop registerShop(String username, ShopRegistrationRequest form, MultipartFile logo) {
        User owner = userRepository.findByUsername(username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phien dang nhap khong hop le"));

        if (owner.getRole() != Role.USER && owner.getRole() != Role.VENDOR) {
            throw new AccessDeniedException("Chi tai khoan khach hang moi duoc dang ky shop");
        }
        if (shopRepository.findByOwner(owner).isPresent()) {
            throw new IllegalStateException("Tai khoan nay da dang ky shop roi");
        }

        Shop shop = new Shop();
        shop.setOwner(owner);
        shop.setName(form.getName().trim());
        shop.setDescription(trimToNull(form.getDescription()));
        shop.setAddress(form.getAddress().trim());
        shop.setPhone(trimToNull(form.getPhone()));
        shop.setStatus(ShopStatus.PENDING);
        shop.setCommissionRate(DEFAULT_COMMISSION_RATE);

        if (imageUploadValidator.validateIfPresent(logo)) {
            shop.setLogoUrl(cloudinaryService.upload(logo, SHOP_IMAGE_FOLDER));
        }
        return shopRepository.save(shop);
    }

    /**
     * Vendor sua thong tin shop cua minh (ten, mo ta, dia chi, dien thoai, logo, banner).
     * Trang thai va chiet khau KHONG nam trong du lieu nhan vao nen khong the bi sua o day.
     */
    @Transactional
    public Shop updateShop(String username, Long shopId, ShopUpdateRequest form,
                           MultipartFile logo, MultipartFile banner) {
        Shop shop = requireOwnedShop(username, shopId);

        if (shop.getStatus() == ShopStatus.SUSPENDED) {
            throw new AccessDeniedException("Shop dang bi tam dung, khong sua duoc thong tin");
        }

        shop.setName(form.getName().trim());
        shop.setDescription(trimToNull(form.getDescription()));
        shop.setAddress(form.getAddress().trim());
        shop.setPhone(trimToNull(form.getPhone()));

        if (imageUploadValidator.validateIfPresent(logo)) {
            shop.setLogoUrl(cloudinaryService.upload(logo, SHOP_IMAGE_FOLDER));
        }
        if (imageUploadValidator.validateIfPresent(banner)) {
            shop.setBannerUrl(cloudinaryService.upload(banner, SHOP_IMAGE_FOLDER));
        }
        return shopRepository.save(shop);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}