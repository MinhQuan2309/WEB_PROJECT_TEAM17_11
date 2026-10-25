package com.utetra.module.admin;

import com.utetra.entity.Role;
import com.utetra.entity.Shop;
import com.utetra.entity.ShopStatus;
import com.utetra.entity.User;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Kiem duyet shop, dung chung cho khu Admin va khu Manager.
 *
 * Vong doi trang thai cua mot shop:
 *
 *     PENDING  --duyet-->  ACTIVE  --tam dung-->  SUSPENDED --mo lai--> ACTIVE
 *        |
 *        +--tu choi--> REJECTED --mo lai ho so--> PENDING
 *
 * Hai quy tac quan trong:
 *
 *   1. Chi khi shop duoc duyet thi chu shop moi duoc nang tu USER len VENDOR.
 *      Dang ky xong ma chua duyet thi van la USER, khong vao duoc khu /vendor.
 *      Day dung la hop dong ma Thien ghi trong VendorShopService.
 *   2. Manager chi dung duoc vao shop minh dang quan ly, hoac shop chua ai nhan.
 *      Duyet mot shop chua ai nhan thi tu dong nhan luon shop do.
 *      Admin khong bi gioi han nay.
 */
@Service
public class ShopModerationService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;

    public ShopModerationService(ShopRepository shopRepository, UserRepository userRepository) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User requireActor(String username) {
        return userRepository.findByUsername(username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phien dang nhap khong hop le"));
    }

    /**
     * Danh sach shop ma nguoi dang dang nhap duoc nhin thay.
     *
     * @param status loc theo trang thai, null = tat ca
     */
    @Transactional(readOnly = true)
    public List<Shop> visibleShops(User actor, ShopStatus status) {
        boolean admin = actor.getRole() == Role.ADMIN;
        if (admin) {
            return status == null
                    ? shopRepository.findAllByOrderByIdDesc()
                    : shopRepository.findByStatusOrderByIdDesc(status);
        }
        return status == null
                ? shopRepository.findByManagerOrManagerIsNullOrderByIdDesc(actor)
                : shopRepository.findByStatusAndManagerOrManagerIsNullOrderByIdDesc(status, actor);
    }

    @Transactional(readOnly = true)
    public long countByStatus(ShopStatus status) {
        return shopRepository.countByStatus(status);
    }

    /** Duyet shop: PENDING hoac REJECTED -> ACTIVE, va nang chu shop len VENDOR. */
    @Transactional
    public Shop approve(User actor, Long shopId) {
        Shop shop = requireShopInScope(actor, shopId);
        if (shop.getStatus() == ShopStatus.ACTIVE) {
            throw new IllegalStateException("Shop nay da duoc duyet roi");
        }
        if (shop.getStatus() == ShopStatus.SUSPENDED) {
            throw new IllegalStateException("Shop dang bi tam dung, dung nut mo lai thay vi duyet");
        }

        shop.setStatus(ShopStatus.ACTIVE);
        shop.setModerationNote(null);

        // Manager duyet shop chua ai nhan thi nhan luon shop do
        if (actor.getRole() == Role.MANAGER && shop.getManager() == null) {
            shop.setManager(actor);
        }

        User owner = shop.getOwner();
        if (owner != null && owner.getRole() == Role.USER) {
            owner.setRole(Role.VENDOR);
            userRepository.save(owner);
        }
        return shopRepository.save(shop);
    }

    /** Tu choi ho so: PENDING -> REJECTED. Chu shop van la USER. */
    @Transactional
    public Shop reject(User actor, Long shopId, String reason) {
        Shop shop = requireShopInScope(actor, shopId);
        if (shop.getStatus() != ShopStatus.PENDING) {
            throw new IllegalStateException("Chi tu choi duoc ho so dang cho duyet");
        }
        shop.setStatus(ShopStatus.REJECTED);
        shop.setModerationNote(requireReason(reason));
        return shopRepository.save(shop);
    }

    /** Tam dung shop dang hoat dong. Chu shop giu nguyen vai VENDOR. */
    @Transactional
    public Shop suspend(User actor, Long shopId, String reason) {
        Shop shop = requireShopInScope(actor, shopId);
        if (shop.getStatus() != ShopStatus.ACTIVE) {
            throw new IllegalStateException("Chi tam dung duoc shop dang hoat dong");
        }
        shop.setStatus(ShopStatus.SUSPENDED);
        shop.setModerationNote(requireReason(reason));
        return shopRepository.save(shop);
    }

    /** Mo lai shop bi tam dung: SUSPENDED -> ACTIVE. */
    @Transactional
    public Shop reactivate(User actor, Long shopId) {
        Shop shop = requireShopInScope(actor, shopId);
        if (shop.getStatus() != ShopStatus.SUSPENDED) {
            throw new IllegalStateException("Chi mo lai duoc shop dang bi tam dung");
        }
        shop.setStatus(ShopStatus.ACTIVE);
        shop.setModerationNote(null);
        return shopRepository.save(shop);
    }

    /**
     * Mo lai ho so bi tu choi: REJECTED -> PENDING, de chu shop sua thong tin roi duyet lai.
     *
     * TODO ban giao cho Thien: them nut "gui lai ho so" ben khu /vendor de chu shop
     * tu chuyen REJECTED -> PENDING, khoi phai nho Admin mo ho.
     */
    @Transactional
    public Shop reopen(User actor, Long shopId) {
        Shop shop = requireShopInScope(actor, shopId);
        if (shop.getStatus() != ShopStatus.REJECTED) {
            throw new IllegalStateException("Chi mo lai duoc ho so dang bi tu choi");
        }
        shop.setStatus(ShopStatus.PENDING);
        shop.setModerationNote(null);
        return shopRepository.save(shop);
    }

    /** Gan nguoi quan ly cho shop. Chi Admin lam duoc. */
    @Transactional
    public Shop assignManager(User actor, Long shopId, Long managerId) {
        if (actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Chi Admin moi duoc phan cong nguoi quan ly");
        }
        Shop shop = findShop(shopId);
        if (managerId == null) {
            shop.setManager(null);
            return shopRepository.save(shop);
        }
        User manager = userRepository.findById(managerId).orElseThrow(() ->
                new IllegalStateException("Khong tim thay tai khoan #" + managerId));
        if (manager.getRole() != Role.MANAGER) {
            throw new IllegalStateException("Tai khoan " + manager.getUsername() + " khong phai Manager");
        }
        shop.setManager(manager);
        return shopRepository.save(shop);
    }

    /**
     * Dat phan tram chiet khau san thu tren moi don cua shop.
     * Admin dat cho moi shop, Manager chi dat duoc cho shop minh quan ly.
     */
    @Transactional
    public Shop setCommissionRate(User actor, Long shopId, java.math.BigDecimal rate) {
        if (rate == null || rate.compareTo(java.math.BigDecimal.ZERO) < 0
                || rate.compareTo(new java.math.BigDecimal("100")) > 0) {
            throw new IllegalStateException("Chiet khau phai nam trong khoang 0 den 100");
        }
        Shop shop = requireShopInScope(actor, shopId);
        shop.setCommissionRate(rate);
        return shopRepository.save(shop);
    }

    /** Danh sach Manager de Admin chon khi phan cong. */
    @Transactional(readOnly = true)
    public List<User> allManagers() {
        return userRepository.findByRole(Role.MANAGER);
    }

    private Shop findShop(Long shopId) {
        return shopRepository.findById(shopId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay shop #" + shopId));
    }

    /**
     * Lay shop va kiem tra nguoi thao tac co quyen dung vao khong.
     * Admin: moi shop. Manager: shop cua minh hoac shop chua ai nhan.
     */
    private Shop requireShopInScope(User actor, Long shopId) {
        Shop shop = findShop(shopId);
        if (actor.getRole() == Role.ADMIN) {
            return shop;
        }
        if (actor.getRole() != Role.MANAGER) {
            throw new AccessDeniedException("Tai khoan nay khong duoc kiem duyet shop");
        }
        User manager = shop.getManager();
        if (manager != null && !manager.getId().equals(actor.getId())) {
            throw new AccessDeniedException(
                    "Shop #" + shopId + " do nguoi khac quan ly");
        }
        return shop;
    }

    private String requireReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new IllegalStateException("Phai ghi ly do");
        }
        return reason.trim();
    }
}
