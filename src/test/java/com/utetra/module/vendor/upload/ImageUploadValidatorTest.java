package com.utetra.module.vendor.upload;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem tra cua upload anh: chi nhan file anh that, chan file gia dang anh.
 * Chay thuan unit test, khong can Spring context.
 */
class ImageUploadValidatorTest {

    private final ImageUploadValidator validator = new ImageUploadValidator();

    private static final byte[] PNG_HEADER = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};

    private static final byte[] JPEG_HEADER = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1};

    private MockMultipartFile file(String originalName, String contentType, byte[] content) {
        return new MockMultipartFile("file", originalName, contentType, content);
    }

    @Test
    @DisplayName("Khong chon file thi bo qua, khong bao loi")
    void khongChonFileThiBoQua() {
        assertFalse(validator.validateIfPresent(null));
        assertFalse(validator.validateIfPresent(file("", "application/octet-stream", new byte[0])));
    }

    @Test
    @DisplayName("Anh PNG/JPEG that thi nhan")
    void anhThatThiNhan() {
        assertTrue(validator.validateIfPresent(file("logo.png", "image/png", PNG_HEADER)));
        assertTrue(validator.validateIfPresent(file("banner.jpg", "image/jpeg", JPEG_HEADER)));
    }

    @Test
    @DisplayName("File .png nhung noi dung khong phai anh thi bi chan (magic bytes)")
    void fileGiaDangAnhBiChan() {
        byte[] script = "<?php system($_GET['c']); ?>".getBytes(StandardCharsets.UTF_8);
        InvalidImageException ex = assertThrows(InvalidImageException.class,
                () -> validator.validateIfPresent(file("shell.png", "image/png", script)));
        assertTrue(ex.getMessage().contains("dinh dang anh"));
    }

    @Test
    @DisplayName("Content-Type khong phai anh thi bi chan")
    void contentTypKhongPhaiAnhBiChan() {
        assertThrows(InvalidImageException.class,
                () -> validator.validateIfPresent(file("logo.png", "application/pdf", PNG_HEADER)));
    }

    @Test
    @DisplayName("Duoi file la thi bi chan, du Content-Type khai la anh")
    void duoiFileLaBiChan() {
        assertThrows(InvalidImageException.class,
                () -> validator.validateIfPresent(file("logo.exe", "image/png", PNG_HEADER)));
    }

    @Test
    @DisplayName("Ten file chua duong dan thi bi chan")
    void tenFileChuaDuongDanBiChan() {
        assertThrows(InvalidImageException.class,
                () -> validator.validateIfPresent(file("../../etc/passwd.png", "image/png", PNG_HEADER)));
    }

    @Test
    @DisplayName("Anh qua 5MB thi bi chan")
    void anhQuaNangBiChan() {
        byte[] quaNang = new byte[6 * 1024 * 1024];
        System.arraycopy(PNG_HEADER, 0, quaNang, 0, PNG_HEADER.length);
        assertThrows(InvalidImageException.class,
                () -> validator.validateIfPresent(file("to.png", "image/png", quaNang)));
    }
}