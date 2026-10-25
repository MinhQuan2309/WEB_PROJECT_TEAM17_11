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
