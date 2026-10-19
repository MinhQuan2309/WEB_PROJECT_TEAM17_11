package com.utetra.module.vendor;

import com.utetra.entity.Shop;
import com.utetra.entity.Topping;
import com.utetra.module.vendor.dto.ToppingForm;
import com.utetra.repository.CartItemRepository;
import com.utetra.repository.ToppingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Nghiep vu topping cua shop (tuan 2).
 *
 * Topping thuoc ve shop, nen moi thao tac ghi deu kiem tra chu so huu truoc (chan IDOR).
 * Topping da duoc khach dat trong gio hang thi khong xoa cung duoc, chi tat di.
 */
@Service
public class VendorToppingService {

    private final ToppingRepository toppingRepository;
    private final CartItemRepository cartItemRepository;
    private final VendorShopService vendorShopService;

    public VendorToppingService(ToppingRepository toppingRepository,
                                CartItemRepository cartItemRepository,
                                VendorShopService vendorShopService) {
        this.toppingRepository = toppingRepository;
        this.cartItemRepository = cartItemRepository;
        this.vendorShopService = vendorShopService;
    }

    @Transactional(readOnly = true)
    public List<Topping> listMyToppings(String username) {
        Shop shop = vendorShopService.requireMyShop(username);
        return toppingRepository.findByShopOrderByIdAsc(shop);
    }

    @Transactional(readOnly = true)
    public Topping requireOwnedTopping(String username, Long toppingId) {
        Topping topping = toppingRepository.findById(toppingId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay topping #" + toppingId));
        if (topping.getShop() == null
                || topping.getShop().getOwner() == null
                || !topping.getShop().getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException(
                    "Topping #" + toppingId + " khong thuoc shop cua tai khoan " + username);
        }
        return topping;
    }

    @Transactional
    public Topping create(String username, ToppingForm form) {
        Shop shop = vendorShopService.requireMyShop(username);
        Topping topping = new Topping();
        topping.setShop(shop);
        applyForm(topping, form);
        return toppingRepository.save(topping);
    }

    @Transactional
    public Topping update(String username, Long toppingId, ToppingForm form) {
        Topping topping = requireOwnedTopping(username, toppingId);
        applyForm(topping, form);
        return toppingRepository.save(topping);
    }

    @Transactional
    public Topping toggleActive(String username, Long toppingId) {
        Topping topping = requireOwnedTopping(username, toppingId);
        topping.setActive(!topping.isActive());
        return toppingRepository.save(topping);
    }

    @Transactional
    public void delete(String username, Long toppingId) {
        Topping topping = requireOwnedTopping(username, toppingId);
        if (cartItemRepository.countCartItemsUsingTopping(toppingId) > 0) {
            throw new IllegalStateException("Topping \"" + topping.getName()
                    + "\" dang co trong gio hang cua khach nen khong xoa duoc. Hay dung chuc nang Tat.");
        }
        toppingRepository.delete(topping);
    }

    private void applyForm(Topping topping, ToppingForm form) {
        topping.setName(form.getName().trim());
        topping.setPrice(form.getPrice() == null ? BigDecimal.ZERO : form.getPrice());
        topping.setActive(form.isActive());
    }
}