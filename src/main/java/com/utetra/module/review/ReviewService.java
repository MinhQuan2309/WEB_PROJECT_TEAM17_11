package com.utetra.module.review;

import com.utetra.entity.*;
import com.utetra.module.media.CloudinaryService;
import com.utetra.module.review.dto.ReviewFormRequest;
import com.utetra.repository.OrderRepository;
import com.utetra.repository.ProductRepository;
import com.utetra.repository.ReviewRepository;
import com.utetra.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Nghiep vu danh gia (1-5 sao) va binh luan (>= 50 ky tu + anh/video Cloudinary)
 * cho san pham da mua (tuan 5).
 */
@Service
@Transactional
public class ReviewService {

    public static final int MIN_COMMENT_LENGTH = 50;

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    public ReviewService(ReviewRepository reviewRepository,
                         ProductRepository productRepository,
                         OrderRepository orderRepository,
                         UserRepository userRepository,
                         CloudinaryService cloudinaryService) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional(readOnly = true)
    public User getRequiredUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản: " + username));
    }

    @Transactional(readOnly = true)
    public Product getRequiredProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
    }

    /** Loc danh sach don hang da giao (DELIVERED) cua khach co chua san pham can danh gia. */
    @Transactional(readOnly = true)
    public List<Order> findDeliveredOrdersWithProduct(User user, Product product) {
        List<Order> deliveredOrders = orderRepository
                .findByCustomerAndStatusOrderByIdDesc(user, OrderStatus.DELIVERED, PageRequest.of(0, 200))
                .getContent();

        return deliveredOrders.stream()
                .filter(order -> order.getItems().stream()
                        .anyMatch(item -> item.getProduct() != null
                                && item.getProduct().getId().equals(product.getId())))
                .toList();
    }

    /** Tao danh gia moi, upload media len Cloudinary va cap nhat diem trung binh cua san pham. */
    public Review createReview(User author, ReviewFormRequest req, MultipartFile mediaFile) {
        Product product = getRequiredProduct(req.getProductId());

        List<Order> qualifyingOrders = findDeliveredOrdersWithProduct(author, product);
        if (qualifyingOrders.isEmpty()) {
            throw new IllegalStateException("Bạn chỉ có thể đánh giá và bình luận khi đã mua sản phẩm này và đơn hàng ở trạng thái Đã giao.");
        }

        Order targetOrder = qualifyingOrders.get(0);
        if (req.getOrderId() != null) {
            targetOrder = qualifyingOrders.stream()
                    .filter(o -> o.getId().equals(req.getOrderId()))
                    .findFirst()
                    .orElse(qualifyingOrders.get(0));
        }

        if (req.getRating() < 1 || req.getRating() > 5) {
            throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5 sao.");
        }

        String cleanContent = req.getContent() != null ? req.getContent().trim() : "";
        if (cleanContent.length() < MIN_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Nội dung bình luận phải có tối thiểu "
                    + MIN_COMMENT_LENGTH + " ký tự (hiện tại: " + cleanContent.length() + " ký tự).");
        }

        // Xu ly upload file anh/video qua CloudinaryService hoac nhan link truc tiep
        String finalMediaUrl = null;
        if (mediaFile != null && !mediaFile.isEmpty()) {
            if (cloudinaryService.isConfigured()) {
                finalMediaUrl = cloudinaryService.upload(mediaFile, "reviews");
            } else if (req.getMediaUrl() != null && !req.getMediaUrl().isBlank()) {
                finalMediaUrl = req.getMediaUrl().trim();
            } else {
                throw new IllegalStateException(
                        "Máy hiện chưa cấu hình Cloudinary trong application-local.properties. "
                        + "Vui lòng điền key Cloudinary hoặc dán link ảnh/video minh họa vào ô URL.");
            }
        } else if (req.getMediaUrl() != null && !req.getMediaUrl().isBlank()) {
            finalMediaUrl = req.getMediaUrl().trim();
        }

        Review review = new Review();
        review.setProduct(product);
        review.setAuthor(author);
        review.setOrder(targetOrder);
        review.setRating(req.getRating());
        review.setContent(cleanContent);
        review.setMediaUrl(finalMediaUrl);

        Review saved = reviewRepository.save(review);

        // Dong bo lai diem trung binh ratingAvg va so luot danh gia ratingCount cua san pham
        Double avg = reviewRepository.averageRating(product);
        long count = reviewRepository.countByProduct(product);
        product.setRatingAvg(BigDecimal.valueOf(avg != null ? avg : 5.0).setScale(2, RoundingMode.HALF_UP));
        product.setRatingCount((int) count);
        productRepository.save(product);

        return saved;
    }

    public boolean isCloudinaryReady() {
        return cloudinaryService.isConfigured();
    }
}
