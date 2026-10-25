package com.utetra.module.admin;

import com.utetra.entity.Role;
import com.utetra.entity.Shop;
import com.utetra.entity.User;
import com.utetra.repository.ShopRepository;
import com.utetra.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Quan ly nguoi dung cho Admin: tim kiem, khoa/mo khoa, doi vai tro.
 *
 * Hai lan can de Admin khong tu khoa chinh minh ra khoi he thong:
 *   - khong tu khoa tai khoan cua minh
 *   - khong tu ha vai cua chinh minh
 */
@Service
public class UserAdminService {

    public static final int PAGE_SIZE = 15;

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;

    public UserAdminService(UserRepository userRepository, ShopRepository shopRepository) {
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
    }

    @Transactional(readOnly = true)
    public Page<User> search(String keyword, Role role, int page) {
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return userRepository.search(kw, role,
                PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id")));
    }

    @Transactional
    public User toggleLock(String actorUsername, Long userId) {
        User target = require(userId);
        if (target.getUsername().equals(actorUsername)) {
            throw new IllegalStateException("Khong the tu khoa tai khoan cua chinh minh");
        }
        target.setLocked(!target.isLocked());
        return userRepository.save(target);
    }

    @Transactional
    public User changeRole(String actorUsername, Long userId, Role newRole) {
        User target = require(userId);
        if (target.getUsername().equals(actorUsername)) {
            throw new IllegalStateException("Khong the tu doi vai tro cua chinh minh");
        }
        if (newRole == null) {
            throw new IllegalStateException("Chua chon vai tro moi");
        }
        if (target.getRole() == Role.VENDOR && newRole != Role.VENDOR) {
            Optional<Shop> shop = shopRepository.findByOwner(target);
            if (shop.isPresent()) {
                throw new IllegalStateException(
                        "Tai khoan nay dang so huu shop " + shop.get().getName()
                        + ". Hay tam dung shop truoc khi ha vai tro.");
            }
        }
        target.setRole(newRole);
        return userRepository.save(target);
    }

    @Transactional
    public User setEnabled(String actorUsername, Long userId, boolean enabled) {
        User target = require(userId);
        if (target.getUsername().equals(actorUsername) && !enabled) {
            throw new IllegalStateException("Khong the tu tat tai khoan cua chinh minh");
        }
        target.setEnabled(enabled);
        return userRepository.save(target);
    }

    private User require(Long id) {
        return userRepository.findById(id).orElseThrow(() ->
                new IllegalStateException("Khong tim thay tai khoan #" + id));
    }
}
