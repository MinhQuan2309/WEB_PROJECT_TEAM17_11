package com.utetra.module.vendor.upload;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

/**
 * Cua kiem tra file truoc khi day len Cloudinary (diem giao CloudinaryService cua Quan).
 *
 * Vi sao phai kiem tra o day chu khong sua CloudinaryService: CloudinaryService la file
 * dung chung cua nhom, module vendor chi nen goi lai. Ngoai ra kiem tra ngay tai tang
 * nghiep vu cho phep bao loi ro rang cho nguoi dung thay vi nem exception 500.
 *
 * Ba lop kiem tra, phai qua het:
 *   1. Kich thuoc  <= 5MB
 *   2. Content-Type nam trong danh sach trang (khong tin duoi file)
 *   3. Duoi file nam trong danh sach trang
 *   4. Magic bytes cua noi dung file phai dung dinh dang anh (JPEG/PNG/GIF/WEBP)
 *
 * Lop 4 la quan trong nhat: ke tan cong co the doi ten file .exe thanh .png va khai
 * Content-Type la image/png, nhung 12 byte dau tien se khong khop.
 */
@Component
public class ImageUploadValidator {

    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp", "gif");

    /**
     * @return true neu co file de upload, false neu nguoi dung khong chon file nao.
     * @throws InvalidImageException neu co file nhung khong phai anh hop le.
     */
    public boolean validateIfPresent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new InvalidImageException("Anh toi da 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new InvalidImageException("Chi nhan file anh JPEG, PNG, WEBP hoac GIF");
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidImageException("Duoi file phai la jpg, jpeg, png, webp hoac gif");
        }

        if (!looksLikeImage(file)) {
            throw new InvalidImageException("Noi dung file khong dung dinh dang anh");
        }
        return true;
    }

    /** Ten file khong bao gio duoc dung lam duong dan (Cloudinary tu sinh public_id),
     *  nhung van chan ky tu duong dan de tranh nham lan ve sau. */
    public static boolean hasUnsafeFileName(String originalFilename) {
        return originalFilename != null
                && (originalFilename.contains("/") || originalFilename.contains("\\")
                    || originalFilename.contains(".."));
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        if (hasUnsafeFileName(originalFilename)) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private boolean looksLikeImage(MultipartFile file) {
        byte[] head;
        try (InputStream in = file.getInputStream()) {
            head = in.readNBytes(12);
        } catch (IOException ex) {
            throw new InvalidImageException("Khong doc duoc file upload");
        }
        return isJpeg(head) || isPng(head) || isGif(head) || isWebp(head);
    }

    private boolean isJpeg(byte[] h) {
        return h.length >= 3
                && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] h) {
        return h.length >= 8
                && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
                && (h[4] & 0xFF) == 0x0D && (h[5] & 0xFF) == 0x0A
                && (h[6] & 0xFF) == 0x1A && (h[7] & 0xFF) == 0x0A;
    }

    private boolean isGif(byte[] h) {
        return h.length >= 6
                && h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8'
                && (h[4] == '7' || h[4] == '9') && h[5] == 'a';
    }

    private boolean isWebp(byte[] h) {
        return h.length >= 12
                && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
    }
}