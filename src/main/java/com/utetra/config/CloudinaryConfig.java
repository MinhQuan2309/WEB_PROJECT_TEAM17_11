package com.utetra.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bean Cloudinary chi duoc tao khi da khai bao cloudinary.cloud-name
 * trong application-local.properties. Chua co key thi app van chay binh thuong,
 * chi la khong upload duoc anh.
 */
@Configuration
public class CloudinaryConfig {

    @Bean
    @ConditionalOnProperty(name = "cloudinary.cloud-name")
    public Cloudinary cloudinary(
            @org.springframework.beans.factory.annotation.Value("${cloudinary.cloud-name}") String cloudName,
            @org.springframework.beans.factory.annotation.Value("${cloudinary.api-key}") String apiKey,
            @org.springframework.beans.factory.annotation.Value("${cloudinary.api-secret}") String apiSecret) {

        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
    }
}
