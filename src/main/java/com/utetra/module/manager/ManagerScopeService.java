package com.utetra.module.manager;

import com.utetra.entity.Product;
import com.utetra.entity.Role;
import com.utetra.entity.Shop;
import com.utetra.entity.User;
import com.utetra.repository.OrderRepository;
import com.utetra.repository.ProductRepository;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pham vi quan ly cua mot Manager.
 *
 * De tai ghi Manager co cung danh sach chuc nang voi Admin nhung "quan ly tung cua hang".
 * Nhom chot cach hieu nhu sau:
 *
 *   - Danh muc, nha van chuyen, khuyen mai cua san la viec toan san  -> chi Admin
 *   - Shop, san pham cua shop, chu shop, chiet khau cua shop         -> Manager lam
 *     trong pham vi cac shop minh phu trach (Shop.manager)
 *
 * Moi truy van o day deu loc theo danh sach shop cua Manager, nen Manager A
 * khong bao gio nhin thay du lieu cua Manager B.
 */
@Service
public class ManagerScopeService {

    public static final int PAGE_SIZE = 15;

    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public ManagerScopeService(ShopRepository shopRepository,
                               ProductRepository productRepository,
                               OrderRepository orderRepository,
                               UserRepository userRepository) {
        this.shopRepository = shopRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User requireActor(String username) {
        return userRepository.findByUsername(username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phien dang nhap khong hop le"));
    }

    /** Cac shop nguoi nay phu trach. Admin thay tat ca. */
    @Transactional(readOnly = true)
    public List<Shop> shopsOf(User actor) {
        if (actor.getRole() == Role.ADMIN) {
            return shopRepository.findAllByOrderByIdDesc();
        }
        return shopRepository.findByManager(actor);
    }

    @Transactional(readOnly = true)
    public Page<Product> productsOf(User actor, int page) {
        List<Shop> shops = shopsOf(actor);
        if (shops.isEmpty()) {
            return Page.empty();
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE);
        return productRepository.findByShopInOrderByIdDesc(shops, pageable);
    }

    /** Chu cua cac shop trong pham vi, de Manager biet lien he voi ai. */
    @Transactional(readOnly = true)
    public List<User> shopOwnersOf(User actor) {
        return shopsOf(actor).stream()
                .map(Shop::getOwner)
                .filter(owner -> owner != null)
                .distinct()
                .toList();
    }

    /**
     * An hoac hien mot san pham vi pham. Chi lam duoc voi san pham thuoc shop
     * trong pham vi cua minh.
     */
    @Transactional
    public Product toggleProductActive(User actor, Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() ->
                new IllegalStateException("Khong tim thay san pham #" + productId));

        if (actor.getRole() != Role.ADMIN) {
            List<Shop> shops = shopsOf(actor);
            boolean inScope = product.getShop() != null
                    && shops.stream().anyMatch(s -> s.getId().equals(product.getShop().getId()));
            if (!inScope) {
                throw new AccessDeniedException(
                        "San pham nay thuoc shop khong nam trong pham vi quan ly cua ban");
            }
        }
        product.setActive(!product.isActive());
        return productRepository.save(product);
    }

    // ===================================================== phan cong shipper

    /** Cac trang thai cho thay shipper dang con don do dang tren tay. */
    private static final List<com.utetra.entity.OrderStatus> DANG_GIAO = List.of(
            com.utetra.entity.OrderStatus.CONFIRMED,
            com.utetra.entity.OrderStatus.PICKED_UP,
            com.utetra.entity.OrderStatus.SHIPPING);

    /**
     * Danh sach tai khoan Manager duoc phep dat lam shipper.
     *
     * Chi gom nguoi dung thuong va shipper hien co. Khong liet ke ADMIN, MANAGER
     * hay VENDOR de Manager khong the dung man hinh nay ma dong vao cac vai khac.
     */
    @Transactional(readOnly = true)
    public List<User> shipperCandidates() {
        return userRepository.findByRoleInOrderByUsernameAsc(List.of(Role.USER, Role.SHIPPER));
    }

    /**
     * Dat mot tai khoan thuong lam shipper, hoac go vai shipper tra ve nguoi dung thuong.
     *
     * Ba lan can:
     *   - chi ADMIN va MANAGER duoc goi,
     *   - khong tu doi vai tro cua chinh minh,
     *   - chi doi qua lai giua USER va SHIPPER, khong dong toi ADMIN / MANAGER / VENDOR.
     *
     * Rieng khi go vai shipper: chan neu nguoi do con don dang giao do dang, neu khong
     * nhung don ay se khong con ai phu trach.
     */
    @Transactional
    public User toggleShipperRole(User actor, Long userId) {
        if (actor.getRole() != Role.ADMIN && actor.getRole() != Role.MANAGER) {
            throw new AccessDeniedException("Chi Admin hoac Manager moi duoc phan cong shipper");
        }

        User target = userRepository.findById(userId).orElseThrow(() ->
                new IllegalStateException("Khong tim thay tai khoan #" + userId));

        if (target.getId().equals(actor.getId())) {
            throw new IllegalStateException("Khong the tu doi vai tro cua chinh minh");
        }
        if (target.getRole() != Role.USER && target.getRole() != Role.SHIPPER) {
            throw new IllegalStateException("Chi dat duoc shipper cho tai khoan nguoi dung thuong. "
                    + "Tai khoan " + target.getUsername() + " dang la " + target.getRole() + ".");
        }

        if (target.getRole() == Role.SHIPPER) {
            long dangGiao = orderRepository.countByShipperAndStatusIn(target, DANG_GIAO);
            if (dangGiao > 0) {
                throw new IllegalStateException("Shipper " + target.getUsername() + " con "
                        + dangGiao + " don dang giao, khong go vai duoc. Hay cho giao xong truoc.");
            }
            target.setRole(Role.USER);
        } else {
            target.setRole(Role.SHIPPER);
        }
        return userRepository.save(target);
    }

    /** So lieu tong quan cho dashboard cua Manager. */
    @Transactional(readOnly = true)
    public ManagerSummary summaryOf(User actor) {
        List<Shop> shops = shopsOf(actor);
        if (shops.isEmpty()) {
            return new ManagerSummary(0, 0, 0, 0, BigDecimal.ZERO);
        }
        return new ManagerSummary(
                shops.size(),
                shopOwnersOf(actor).size(),
                productRepository.countByShopIn(shops),
                orderRepository.countByShopIn(shops),
                orderRepository.revenueOfShops(shops));
    }

    /** So lieu hien tren dashboard cua Manager. */
    public record ManagerSummary(long shopCount,
                                 long ownerCount,
                                 long productCount,
                                 long orderCount,
                                 BigDecimal revenue) {
    }
}
