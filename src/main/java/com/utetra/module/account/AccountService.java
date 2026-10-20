package com.utetra.module.account;

import com.utetra.entity.*;
import com.utetra.module.account.dto.AddressFormRequest;
import com.utetra.module.account.dto.PasswordChangeRequest;
import com.utetra.module.account.dto.ProfileUpdateRequest;
import com.utetra.module.media.CloudinaryService;
import com.utetra.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * Service xu ly ho so ca nhan, doi mat khau, quan ly nhieu dia chi nhan hang,
 * danh sach san pham yeu thich (Wishlist) va lich su san pham da xem (ViewHistory).
 */
@Service
@Transactional
public class AccountService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final WishlistRepository wishlistRepository;
    private final ViewHistoryRepository viewHistoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;

    public AccountService(UserRepository userRepository,
                          AddressRepository addressRepository,
                          WishlistRepository wishlistRepository,
                          ViewHistoryRepository viewHistoryRepository,
                          ProductRepository productRepository,
                          PasswordEncoder passwordEncoder,
                          CloudinaryService cloudinaryService) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.wishlistRepository = wishlistRepository;
        this.viewHistoryRepository = viewHistoryRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional(readOnly = true)
    public User getRequiredUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay tai khoan: " + username));
    }

    public void updateProfile(User user, ProfileUpdateRequest req, MultipartFile avatarFile) {
        Optional<User> existingEmail = userRepository.findByEmail(req.getEmail().trim());
        if (existingEmail.isPresent() && !existingEmail.get().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Email nay da duoc su dung boi tai khoan khac.");
        }

        user.setFullName(req.getFullName().trim());
        user.setEmail(req.getEmail().trim());
        user.setPhone(req.getPhone() != null ? req.getPhone().trim() : null);

        if (avatarFile != null && !avatarFile.isEmpty() && cloudinaryService.isConfigured()) {
            String uploadedUrl = cloudinaryService.upload(avatarFile, "avatars");
            if (uploadedUrl != null) {
                user.setAvatarUrl(uploadedUrl);
            }
        } else if (req.getAvatarUrl() != null && !req.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(req.getAvatarUrl().trim());
        }

        userRepository.save(user);
    }

    public void changePassword(User user, PasswordChangeRequest req) {
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        }
        if (req.getNewPassword() == null || req.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        }
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new IllegalArgumentException("Xác nhận mật khẩu mới không khớp.");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<Address> getAddresses(User user) {
        return addressRepository.findByUserOrderByDefaultAddressDescIdDesc(user);
    }

    @Transactional(readOnly = true)
    public Optional<Address> findUserAddress(User user, Long addressId) {
        if (addressId == null) return Optional.empty();
        return addressRepository.findById(addressId)
                .filter(a -> a.getUser().getId().equals(user.getId()));
    }

    public Address saveAddress(User user, AddressFormRequest req) {
        List<Address> existingList = addressRepository.findByUserOrderByDefaultAddressDescIdDesc(user);
        Address address;
        if (req.getId() != null) {
            address = findUserAddress(user, req.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ cần sửa."));
        } else {
            address = new Address();
            address.setUser(user);
        }

        address.setReceiverName(req.getReceiverName().trim());
        address.setPhone(req.getPhone().trim());
        address.setStreet(req.getStreet().trim());
        address.setWard(req.getWard() != null ? req.getWard().trim() : "");
        address.setDistrict(req.getDistrict() != null ? req.getDistrict().trim() : "");
        address.setProvince(req.getProvince() != null ? req.getProvince().trim() : "");

        boolean makeDefault = req.isDefaultAddress() || existingList.isEmpty();
        if (makeDefault) {
            for (Address other : existingList) {
                if (other.isDefaultAddress()) {
                    other.setDefaultAddress(false);
                    addressRepository.save(other);
                }
            }
            address.setDefaultAddress(true);
        } else if (address.getId() == null) {
            address.setDefaultAddress(false);
        }

        return addressRepository.save(address);
    }

    public void setDefaultAddress(User user, Long addressId) {
        Address target = findUserAddress(user, addressId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ."));
        List<Address> all = addressRepository.findByUserOrderByDefaultAddressDescIdDesc(user);
        for (Address a : all) {
            a.setDefaultAddress(a.getId().equals(target.getId()));
        }
        addressRepository.saveAll(all);
    }

    public void deleteAddress(User user, Long addressId) {
        Address target = findUserAddress(user, addressId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ."));
        boolean wasDefault = target.isDefaultAddress();
        addressRepository.delete(target);

        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserOrderByDefaultAddressDescIdDesc(user);
            if (!remaining.isEmpty()) {
                Address first = remaining.get(0);
                first.setDefaultAddress(true);
                addressRepository.save(first);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ViewHistory> getRecentlyViewed(User user) {
        return viewHistoryRepository.findTop10ByUserOrderByViewedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public Page<Wishlist> getWishlistPage(User user, int page, int size) {
        return wishlistRepository.findByUserOrderByIdDesc(user, PageRequest.of(Math.max(page, 0), size));
    }

    public boolean toggleWishlist(User user, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        if (wishlistRepository.existsByUserAndProduct(user, product)) {
            wishlistRepository.deleteByUserAndProduct(user, product);
            product.setLikeCount(Math.max(0, product.getLikeCount() - 1));
            productRepository.save(product);
            return false;
        } else {
            Wishlist w = new Wishlist();
            w.setUser(user);
            w.setProduct(product);
            wishlistRepository.save(w);
            product.setLikeCount(product.getLikeCount() + 1);
            productRepository.save(product);
            return true;
        }
    }
}
