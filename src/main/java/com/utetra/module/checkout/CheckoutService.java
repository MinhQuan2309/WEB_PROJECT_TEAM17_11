package com.utetra.module.checkout;

import com.utetra.entity.*;
import com.utetra.module.cart.CartService;
import com.utetra.module.cart.dto.CartShopGroupDto;
import com.utetra.module.cart.dto.CartSummaryDto;
import com.utetra.module.checkout.dto.CheckoutRequest;
import com.utetra.module.checkout.dto.VoucherPreviewDto;
import com.utetra.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Service xu ly dat hang (tach don theo Shop), kiem tra/ap ma giam gia Voucher,
 * chon nha van chuyen (Carrier) va tao giao dich thanh toan (COD / VNPAY / MOMO).
 */
@Service
@Transactional
public class CheckoutService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final CarrierRepository carrierRepository;
    private final VoucherRepository voucherRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ShopRepository shopRepository;
    private final CartService cartService;

    public CheckoutService(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           AddressRepository addressRepository,
                           CarrierRepository carrierRepository,
                           VoucherRepository voucherRepository,
                           OrderRepository orderRepository,
                           PaymentRepository paymentRepository,
                           ProductRepository productRepository,
                           ProductVariantRepository variantRepository,
                           ShopRepository shopRepository,
                           CartService cartService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.addressRepository = addressRepository;
        this.carrierRepository = carrierRepository;
        this.voucherRepository = voucherRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.shopRepository = shopRepository;
        this.cartService = cartService;
    }

    @Transactional(readOnly = true)
    public List<Carrier> getActiveCarriers() {
        return carrierRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<Voucher> getAvailableVouchers(CartSummaryDto summary) {
        List<Voucher> result = new ArrayList<>(voucherRepository.findByShopIsNullAndActiveTrue());
        if (summary != null && summary.shopGroups() != null) {
            for (CartShopGroupDto group : summary.shopGroups()) {
                shopRepository.findById(group.shopId()).ifPresent(shop ->
                        result.addAll(voucherRepository.findByShopAndActiveTrue(shop)));
            }
        }
        return result;
    }

    /**
     * Tao nhanh 3 ma Voucher mau khi nguoi dung bam nut "Lay ma giam gia dung thu" tren giao dien
     * (khong tao trong ApplicationRunner de giu nguyen unit test goc cua Quan).
     */
    public void ensureSampleVouchersOnDemand() {
        if (!voucherRepository.findByShopIsNullAndActiveTrue().isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();

        Voucher v1 = new Voucher();
        v1.setCode("UTETRA10");
        v1.setDiscountType(DiscountType.PERCENT);
        v1.setValue(new BigDecimal("10"));
        v1.setMinOrderTotal(new BigDecimal("30000"));
        v1.setQuantity(100);
        v1.setStartAt(now.minusDays(1));
        v1.setEndAt(now.plusDays(30));
        v1.setActive(true);

        Voucher v2 = new Voucher();
        v2.setCode("GIAM15K");
        v2.setDiscountType(DiscountType.AMOUNT);
        v2.setValue(new BigDecimal("15000"));
        v2.setMinOrderTotal(new BigDecimal("50000"));
        v2.setQuantity(100);
        v2.setStartAt(now.minusDays(1));
        v2.setEndAt(now.plusDays(30));
        v2.setActive(true);

        Voucher v3 = new Voucher();
        v3.setCode("FREESHIP");
        v3.setDiscountType(DiscountType.FREE_SHIPPING);
        v3.setValue(BigDecimal.ZERO);
        v3.setMinOrderTotal(new BigDecimal("40000"));
        v3.setQuantity(100);
        v3.setStartAt(now.minusDays(1));
        v3.setEndAt(now.plusDays(30));
        v3.setActive(true);

        voucherRepository.saveAll(List.of(v1, v2, v3));
    }

    /**
     * Kiem tra dieu kien ma giam gia Voucher va tinh so tien duoc giam.
     */
    @Transactional(readOnly = true)
    public Voucher validateAndGetVoucher(String voucherCode, BigDecimal subtotal, Set<Long> shopIdsInCheckout) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return null;
        }
        Voucher voucher = voucherRepository.findByCodeAndActiveTrue(voucherCode.trim().toUpperCase())
                .or(() -> voucherRepository.findByCodeAndActiveTrue(voucherCode.trim()))
                .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá \"" + voucherCode + "\" không tồn tại hoặc đã ngừng kích hoạt."));

        LocalDateTime now = LocalDateTime.now();
        if (voucher.getStartAt() != null && now.isBefore(voucher.getStartAt())) {
            throw new IllegalArgumentException("Mã giảm giá \"" + voucher.getCode() + "\" chưa đến thời gian có hiệu lực.");
        }
        if (voucher.getEndAt() != null && now.isAfter(voucher.getEndAt())) {
            throw new IllegalArgumentException("Mã giảm giá \"" + voucher.getCode() + "\" đã hết hạn sử dụng.");
        }
        if (voucher.getQuantity() > 0 && voucher.getUsedCount() >= voucher.getQuantity()) {
            throw new IllegalArgumentException("Mã giảm giá \"" + voucher.getCode() + "\" đã hết lượt sử dụng.");
        }
        if (voucher.getMinOrderTotal() != null && subtotal.compareTo(voucher.getMinOrderTotal()) < 0) {
            throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu ("
                    + voucher.getMinOrderTotal().toBigInteger() + " đ) để áp dụng mã \"" + voucher.getCode() + "\".");
        }
        if (voucher.getShop() != null && (shopIdsInCheckout == null || !shopIdsInCheckout.contains(voucher.getShop().getId()))) {
            throw new IllegalArgumentException("Mã \"" + voucher.getCode() + "\" chỉ áp dụng cho cửa hàng "
                    + voucher.getShop().getName() + ".");
        }
        return voucher;
    }

    public BigDecimal calculateDiscount(Voucher voucher, BigDecimal subtotal, BigDecimal shippingFee) {
        if (voucher == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount = switch (voucher.getDiscountType()) {
            case PERCENT -> subtotal.multiply(voucher.getValue())
                    .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
            case AMOUNT -> voucher.getValue();
            case FREE_SHIPPING -> shippingFee;
        };
        BigDecimal maxPossible = subtotal.add(shippingFee);
        return discount.min(maxPossible).max(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public VoucherPreviewDto previewVoucher(User user, String code, Long carrierId, Long shopId) {
        CartSummaryDto summary = cartService.getCartSummary(user, shopId);
        if (summary.isEmpty()) {
            return new VoucherPreviewDto(false, code, "Giỏ hàng đang trống.", "", BigDecimal.ZERO, BigDecimal.ZERO);
        }
        Carrier carrier = (carrierId != null)
                ? carrierRepository.findById(carrierId).orElse(null)
                : carrierRepository.findByActiveTrue().stream().findFirst().orElse(null);

        BigDecimal shippingPerShop = (carrier != null && carrier.getFee() != null) ? carrier.getFee() : BigDecimal.ZERO;
        BigDecimal totalShipping = shippingPerShop.multiply(BigDecimal.valueOf(summary.shopGroups().size()));
        Set<Long> shopIds = summary.shopGroups().stream().map(CartShopGroupDto::shopId).collect(Collectors.toSet());

        try {
            Voucher voucher = validateAndGetVoucher(code, summary.grandSubtotal(), shopIds);
            if (voucher == null) {
                BigDecimal finalTotal = summary.grandSubtotal().add(totalShipping);
                return new VoucherPreviewDto(false, "", "Chưa nhập mã giảm giá.", "", BigDecimal.ZERO, finalTotal);
            }
            BigDecimal discount = calculateDiscount(voucher, summary.grandSubtotal(), totalShipping);
            BigDecimal finalTotal = summary.grandSubtotal().add(totalShipping).subtract(discount).max(BigDecimal.ZERO);
            return new VoucherPreviewDto(
                    true,
                    voucher.getCode(),
                    "Áp dụng mã " + voucher.getCode() + " thành công!",
                    voucher.getDiscountType().getLabel(),
                    discount,
                    finalTotal
            );
        } catch (IllegalArgumentException ex) {
            BigDecimal finalTotal = summary.grandSubtotal().add(totalShipping);
            return new VoucherPreviewDto(false, code, ex.getMessage(), "", BigDecimal.ZERO, finalTotal);
        }
    }

    /**
     * Dat hang: tach Cart thanh cac Order rieng theo tung Shop, luu OrderItem snapshot,
     * tru ton kho ProductVariant, cap nhat Voucher va tao ban ghi Payment.
     */
    public List<Order> placeOrders(User user, CheckoutRequest req) {
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Giỏ hàng của bạn đang trống."));

        List<CartItem> itemsToOrder = cart.getItems().stream()
                .filter(i -> req.getShopId() == null || i.getProduct().getShop().getId().equals(req.getShopId()))
                .toList();

        if (itemsToOrder.isEmpty()) {
            throw new IllegalArgumentException("Không có sản phẩm nào trong giỏ để đặt hàng.");
        }

        // 1. Xac dinh dia chi giao hang
        String receiverName;
        String receiverPhone;
        String shippingAddressText;

        if (req.getAddressId() != null) {
            Address address = addressRepository.findById(req.getAddressId())
                    .filter(a -> a.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new IllegalArgumentException("Địa chỉ nhận hàng đã chọn không hợp lệ."));
            receiverName = address.getReceiverName();
            receiverPhone = address.getPhone();
            shippingAddressText = address.fullAddress();
        } else if (req.getReceiverName() != null && !req.getReceiverName().isBlank()
                && req.getReceiverPhone() != null && !req.getReceiverPhone().isBlank()
                && req.getStreet() != null && !req.getStreet().isBlank()) {
            Address newAddr = new Address();
            newAddr.setUser(user);
            newAddr.setReceiverName(req.getReceiverName().trim());
            newAddr.setPhone(req.getReceiverPhone().trim());
            newAddr.setStreet(req.getStreet().trim());
            newAddr.setWard(req.getWard() != null ? req.getWard().trim() : "");
            newAddr.setDistrict(req.getDistrict() != null ? req.getDistrict().trim() : "");
            newAddr.setProvince(req.getProvince() != null ? req.getProvince().trim() : "");
            newAddr.setDefaultAddress(addressRepository.findByUserOrderByDefaultAddressDescIdDesc(user).isEmpty());
            addressRepository.save(newAddr);

            receiverName = newAddr.getReceiverName();
            receiverPhone = newAddr.getPhone();
            shippingAddressText = newAddr.fullAddress();
        } else {
            throw new IllegalArgumentException("Vui lòng chọn địa chỉ nhận hàng hoặc nhập địa chỉ mới.");
        }

        // 2. Xac dinh nha van chuyen (Carrier)
        Carrier carrier = null;
        if (req.getCarrierId() != null) {
            carrier = carrierRepository.findById(req.getCarrierId()).orElse(null);
        }
        if (carrier == null) {
            List<Carrier> activeCarriers = carrierRepository.findByActiveTrue();
            if (!activeCarriers.isEmpty()) {
                carrier = activeCarriers.get(0);
            }
        }
        BigDecimal carrierFee = (carrier != null && carrier.getFee() != null) ? carrier.getFee() : BigDecimal.ZERO;

        // 3. Nhom CartItem theo Shop
        Map<Long, List<CartItem>> itemsByShop = new LinkedHashMap<>();
        for (CartItem ci : itemsToOrder) {
            itemsByShop.computeIfAbsent(ci.getProduct().getShop().getId(), k -> new ArrayList<>()).add(ci);
        }

        BigDecimal allShopsSubtotal = BigDecimal.ZERO;
        Map<Long, BigDecimal> subtotalByShop = new HashMap<>();
        for (Map.Entry<Long, List<CartItem>> entry : itemsByShop.entrySet()) {
            BigDecimal shopSub = BigDecimal.ZERO;
            for (CartItem ci : entry.getValue()) {
                BigDecimal unit = computeUnitPrice(ci);
                shopSub = shopSub.add(unit.multiply(BigDecimal.valueOf(ci.getQuantity())));
            }
            subtotalByShop.put(entry.getKey(), shopSub);
            allShopsSubtotal = allShopsSubtotal.add(shopSub);
        }

        // 4. Kiem tra Voucher neu co
        Voucher voucher = validateAndGetVoucher(req.getVoucherCode(), allShopsSubtotal, itemsByShop.keySet());
        boolean voucherUsed = false;

        PaymentMethod paymentMethod = req.getPaymentMethod() != null ? req.getPaymentMethod() : PaymentMethod.COD;
        List<Order> createdOrders = new ArrayList<>();

        for (Map.Entry<Long, List<CartItem>> entry : itemsByShop.entrySet()) {
            List<CartItem> shopCartItems = entry.getValue();
            Shop shop = shopCartItems.get(0).getProduct().getShop();
            BigDecimal shopSubtotal = subtotalByShop.get(shop.getId());

            Order order = new Order();
            order.setCode(generateOrderCode());
            order.setCustomer(user);
            order.setShop(shop);
            order.setCarrier(carrier);
            order.setReceiverName(receiverName);
            order.setReceiverPhone(receiverPhone);
            order.setShippingAddress(shippingAddressText);
            order.setSubtotal(shopSubtotal);
            order.setShippingFee(carrierFee);
            order.setStatus(OrderStatus.NEW);
            order.setPaymentMethod(paymentMethod);
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setNote(req.getNote() != null ? req.getNote().trim() : null);

            // Tinh discount cho Order nay neu Voucher hop le
            BigDecimal orderDiscount = BigDecimal.ZERO;
            if (voucher != null) {
                boolean applicableToThisShop = voucher.getShop() == null
                        || voucher.getShop().getId().equals(shop.getId());
                if (applicableToThisShop && (!voucherUsed || voucher.getDiscountType() == DiscountType.PERCENT)) {
                    orderDiscount = calculateDiscount(voucher, shopSubtotal, carrierFee);
                    order.setVoucher(voucher);
                    voucherUsed = true;
                }
            }
            order.setDiscount(orderDiscount);

            BigDecimal orderTotal = shopSubtotal.add(carrierFee).subtract(orderDiscount).max(BigDecimal.ZERO);
            order.setTotal(orderTotal);

            for (CartItem ci : shopCartItems) {
                // Kiem tra & tru ton kho size
                ProductVariant variant = ci.getVariant();
                if (variant != null) {
                    if (variant.getStock() < ci.getQuantity()) {
                        throw new IllegalArgumentException("Sản phẩm \"" + ci.getProduct().getName()
                                + " (" + variant.getName() + ")\" không đủ tồn kho.");
                    }
                    variant.setStock(variant.getStock() - ci.getQuantity());
                    variantRepository.save(variant);
                }

                // Tang soldCount cua san pham
                Product product = ci.getProduct();
                product.setSoldCount(product.getSoldCount() + ci.getQuantity());
                productRepository.save(product);

                BigDecimal unitPrice = computeUnitPrice(ci);
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity()));

                OrderItem oi = new OrderItem();
                oi.setOrder(order);
                oi.setProduct(product);
                oi.setProductName(product.getName());
                oi.setVariantName(variant != null ? variant.getName() : "Tiêu chuẩn");
                oi.setToppingsText(buildToppingsAndOptionsSnapshot(ci));
                oi.setUnitPrice(unitPrice);
                oi.setQuantity(ci.getQuantity());
                oi.setLineTotal(lineTotal);
                order.getItems().add(oi);
            }

            Order savedOrder = orderRepository.save(order);

            // Tao ban ghi Payment tuong ung (diem giao voi module payment cua Thien)
            Payment payment = new Payment();
            payment.setOrder(savedOrder);
            payment.setMethod(paymentMethod);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setAmount(savedOrder.getTotal());
            paymentRepository.save(payment);

            createdOrders.add(savedOrder);
        }

        if (voucher != null && voucherUsed) {
            voucher.setUsedCount(voucher.getUsedCount() + 1);
            voucherRepository.save(voucher);
        }

        // Xoa cac CartItem da dat khoi gio hang DB
        cart.getItems().removeAll(itemsToOrder);
        cartItemRepository.deleteAll(itemsToOrder);
        cartRepository.save(cart);

        return createdOrders;
    }

    /**
     * Xac nhan ket qua thanh toan tu cong VNPAY / MOMO Sandbox callback.
     */
    public List<Order> confirmGatewayPayment(User user, List<String> orderCodes, boolean success, String gatewayName) {
        List<Order> updated = new ArrayList<>();
        String txId = gatewayName + "-" + System.currentTimeMillis();

        for (String code : orderCodes) {
            orderRepository.findByCode(code)
                    .filter(o -> o.getCustomer().getId().equals(user.getId()))
                    .ifPresent(order -> {
                        Payment payment = paymentRepository.findTopByOrderOrderByIdDesc(order)
                                .orElseGet(() -> {
                                    Payment p = new Payment();
                                    p.setOrder(order);
                                    p.setMethod(order.getPaymentMethod());
                                    p.setAmount(order.getTotal());
                                    return p;
                                });

                        if (success) {
                            order.setPaymentStatus(PaymentStatus.PAID);
                            payment.setStatus(PaymentStatus.PAID);
                            payment.setTransactionId(txId);
                            payment.setPaidAt(LocalDateTime.now());
                            payment.setGatewayResponse("00 - Giao dich thanh cong qua " + gatewayName + " Sandbox");
                        } else {
                            order.setPaymentStatus(PaymentStatus.FAILED);
                            payment.setStatus(PaymentStatus.FAILED);
                            payment.setTransactionId(txId);
                            payment.setGatewayResponse("24 - Khach hang huy giao dich tren cong " + gatewayName);
                        }
                        paymentRepository.save(payment);
                        updated.add(orderRepository.save(order));
                    });
        }
        return updated;
    }

    @Transactional(readOnly = true)
    public List<Order> findUserOrdersByCodes(User user, List<String> codes) {
        if (codes == null || codes.isEmpty()) return Collections.emptyList();
        List<Order> result = new ArrayList<>();
        for (String code : codes) {
            orderRepository.findByCode(code.trim())
                    .filter(o -> o.getCustomer().getId().equals(user.getId()))
                    .ifPresent(result::add);
        }
        return result;
    }

    private BigDecimal computeUnitPrice(CartItem ci) {
        BigDecimal base = ci.getProduct().getBasePrice() != null ? ci.getProduct().getBasePrice() : BigDecimal.ZERO;
        BigDecimal variantExtra = (ci.getVariant() != null && ci.getVariant().getExtraPrice() != null)
                ? ci.getVariant().getExtraPrice()
                : BigDecimal.ZERO;
        BigDecimal toppingExtra = ci.getToppings() != null
                ? ci.getToppings().stream()
                    .map(t -> t.getPrice() != null ? t.getPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;
        return base.add(variantExtra).add(toppingExtra);
    }

    private String buildToppingsAndOptionsSnapshot(CartItem ci) {
        List<String> parts = new ArrayList<>();
        if (ci.getToppings() != null && !ci.getToppings().isEmpty()) {
            String tNames = ci.getToppings().stream().map(Topping::getName).collect(Collectors.joining(", "));
            parts.add("Topping: " + tNames);
        }
        parts.add("Đường " + ci.getSugarLevel() + "%");
        parts.add("Đá " + ci.getIceLevel() + "%");
        if (ci.getNote() != null && !ci.getNote().isBlank()) {
            parts.add("Ghi chú: " + ci.getNote());
        }
        String joined = String.join(" | ", parts);
        return joined.length() > 250 ? joined.substring(0, 250) : joined;
    }

    private String generateOrderCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(100, 999);
        return "UTE" + datePart + rand;
    }
}
