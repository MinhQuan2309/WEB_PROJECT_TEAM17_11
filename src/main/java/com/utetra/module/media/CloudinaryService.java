package com.utetra.module.media;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Diem giao so 1 giua 3 nguoi: Quan viet, Hai dung cho anh/video binh luan,
 * Thien dung cho anh san pham va logo shop.
 *
 *   String url = cloudinaryService.upload(file, "products");
 */
@Service
public class CloudinaryService {

    private final ObjectProvider<Cloudinary> cloudinaryProvider;

    public CloudinaryService(ObjectProvider<Cloudinary> cloudinaryProvider) {
        this.cloudinaryProvider = cloudinaryProvider;
    }

    /**
     * Upload mot file len Cloudinary va tra ve URL dung duoc truc tiep trong the img.
     *
     * @param file   file nhan tu form (co the la anh hoac video)
     * @param folder thu muc tren Cloudinary, vi du "products", "reviews", "shops"
     */
    public String upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        Cloudinary cloudinary = cloudinaryProvider.getIfAvailable();
        if (cloudinary == null) {
            throw new IllegalStateException(
                    "Chua cau hinh Cloudinary. Dien cloudinary.cloud-name / api-key / api-secret "
                    + "vao application-local.properties roi chay lai.");
        }
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "utetra/" + folder,
                            "resource_type", "auto"));
            return String.valueOf(result.get("secure_url"));
        } catch (IOException ex) {
            throw new IllegalStateException("Upload that bai: " + ex.getMessage(), ex);
        }
    }

    public boolean isConfigured() {
        return cloudinaryProvider.getIfAvailable() != null;
    }
}
