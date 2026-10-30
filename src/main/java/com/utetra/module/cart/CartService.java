package com.utetra.module.cart;

import com.utetra.entity.*;
import com.utetra.module.cart.dto.AddToCartRequest;
import com.utetra.module.cart.dto.CartItemViewDto;
import com.utetra.module.cart.dto.CartShopGroupDto;
import com.utetra.module.cart.dto.CartSummaryDto;
import com.utetra.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service quan ly gio hang luu tren CSDL (bang carts va cart_items).
 * Nhom san pham theo tung Shop de ho tro dat hang tach don.
 */
@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ToppingRepository toppingRepository;
    private final UserRepository userRepository;
    private final com.utetra.module.promotion.PromotionPricingService promotionPricingService;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository,
                       ProductVariantRepository variantRepository,
                       ToppingRepository toppingRepository,
                       UserRepository userRepository,
                       com.utetra.module.promotion.PromotionPricingService promotionPricingService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.toppingRepository = toppingRepository;
        this.userRepository = userRepository;
        this.promotionPricingService = promotionPricingService;
    }

    @Transactional(readOnly = true)
    public User getRequiredUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản: " + username));
    }

    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user).orElseGet(() -> {
            Cart created = new Cart();
            created.setUser(user);
            return cartRepository.save(created);
        });
    }

    @Transactional(readOnly = true)
    public CartSummaryDto getCartSummary(User user, Long filterShopId) {
        Optional<Cart> cartOpt = cartRepository.findByUser(user);
        if (cartOpt.isEmpty() || cartOpt.get().getItems().isEmpty()) {
            return new CartSummaryDto(Collections.emptyList(), 0, 0, BigDecimal.ZERO);
        }

        List<CartItem> rawItems = cartOpt.get().getItems().stream()
                .filter(i -> filterShopId == null || i.getProduct().getShop().getId().equals(filterShopId))
                .toList();

        if (rawItems.isEmpty()) {
            return new CartSummaryDto(Collections.emptyList(), 0, 0, BigDecimal.ZERO);
        }

        Map<Long, List<CartItem>> byShop = new LinkedHashMap<>();
        for (CartItem item : rawItems) {
            Long sid = item.getProduct().getShop().getId();
            byShop.computeIfAbsent(sid, k -> new ArrayList<>()).add(item);
        }

        List<CartShopGroupDto> groups = new ArrayList<>();
        int totalLines = 0;
        int totalQty = 0;
        BigDecimal grandSubtotal = BigDecimal.ZERO;

        for (List<CartItem> shopItems : byShop.values()) {
            Shop shop = shopItems.get(0).getProduct().getShop();
            List<CartItemViewDto> viewDtos = shopItems.stream()
                    .map(ci -> CartItemViewDto.from(ci,
                            promotionPricingService.effectiveBasePrice(ci.getProduct())))
                    .toList();
            int shopQty = viewDtos.stream().mapToInt(CartItemViewDto::quantity).sum();
            BigDecimal shopSub = viewDtos.stream()
                    .map(CartItemViewDto::lineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            groups.add(new CartShopGroupDto(
                    shop.getId(),
                    shop.getName(),
                    shop.getAddress(),
                    viewDtos,
                    shopQty,
                    shopSub
            ));

            totalLines += viewDtos.size();
            totalQty += shopQty;
            grandSubtotal = grandSubtotal.add(shopSub);
        }

        return new CartSummaryDto(groups, totalLines, totalQty, grandSubtotal);
    }

    public CartItem addToCart(User user, AddToCartRequest req) {
        Product product = productRepository.findById(req.getProductId())
                .filter(Product::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại hoặc đã ngừng bán."));

        ProductVariant variant = null;
        if (req.getVariantId() != null) {
            variant = variantRepository.findById(req.getVariantId())
                    .filter(v -> v.getProduct().getId().equals(product.getId()))
                    .orElseThrow(() -> new IllegalArgumentException("Kích cỡ (Size) không hợp lệ."));
            if (variant.getStock() <= 0) {
                throw new IllegalArgumentException("Kích cỡ " + variant.getName() + " hiện đã hết hàng.");
            }
        } else {
            List<ProductVariant> variants = variantRepository.findByProduct(product);
            if (!variants.isEmpty()) {
                variant = variants.get(0);
            }
        }

        List<Topping> selectedToppings = new ArrayList<>();
        if (req.getToppingIds() != null && !req.getToppingIds().isEmpty()) {
            selectedToppings = toppingRepository.findAllById(req.getToppingIds()).stream()
                    .filter(t -> t.isActive() && t.getShop().getId().equals(product.getShop().getId()))
                    .sorted(Comparator.comparing(Topping::getId))
                    .collect(Collectors.toList());
        }

        int qty = Math.max(1, req.getQuantity());
        int sugar = clampPercent(req.getSugarLevel());
        int ice = clampPercent(req.getIceLevel());
        String cleanNote = (req.getNote() != null && !req.getNote().isBlank()) ? req.getNote().trim() : null;

        Cart cart = getOrCreateCart(user);

        // Kiem tra neu da co dong CartItem cung Product + Variant + Toppings + Sugar + Ice thi cong don so luong
        Set<Long> requestedToppingIds = selectedToppings.stream().map(Topping::getId).collect(Collectors.toSet());
        for (CartItem existing : cart.getItems()) {
            boolean sameProduct = existing.getProduct().getId().equals(product.getId());
            boolean sameVariant = (existing.getVariant() == null && variant == null)
                    || (existing.getVariant() != null && variant != null && existing.getVariant().getId().equals(variant.getId()));
            Set<Long> existingToppingIds = existing.getToppings().stream().map(Topping::getId).collect(Collectors.toSet());
            boolean sameToppings = existingToppingIds.equals(requestedToppingIds);
            boolean sameSugarIce = existing.getSugarLevel() == sugar && existing.getIceLevel() == ice;

            if (sameProduct && sameVariant && sameToppings && sameSugarIce) {
                existing.setQuantity(existing.getQuantity() + qty);
                if (cleanNote != null) {
                    existing.setNote(cleanNote);
                }
                return cartItemRepository.save(existing);
            }
        }

        CartItem newItem = new CartItem();
        newItem.setCart(cart);
        newItem.setProduct(product);
        newItem.setVariant(variant);
        newItem.setToppings(selectedToppings);
        newItem.setQuantity(qty);
        newItem.setSugarLevel(sugar);
        newItem.setIceLevel(ice);
        newItem.setNote(cleanNote);

        cart.getItems().add(newItem);
        return cartItemRepository.save(newItem);
    }

    public void updateItemQuantity(User user, Long itemId, int quantity) {
        Cart cart = getOrCreateCart(user);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món trong giỏ hàng."));

        if (quantity <= 0) {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
            return;
        }

        if (item.getVariant() != null && item.getVariant().getStock() < quantity) {
            throw new IllegalArgumentException("Số lượng vượt quá tồn kho hiện tại (" + item.getVariant().getStock() + ").");
        }
        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }

    public void removeItem(User user, Long itemId) {
        Cart cart = getOrCreateCart(user);
        cart.getItems().removeIf(i -> i.getId().equals(itemId));
        cartRepository.save(cart);
    }

    public void clearCart(User user) {
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private int clampPercent(int val) {
        if (val < 0) return 0;
        if (val > 100) return 100;
        return val;
    }
}
