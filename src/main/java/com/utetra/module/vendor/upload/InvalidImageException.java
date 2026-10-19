package com.utetra.module.vendor.upload;

/**
 * File nguoi dung upload khong phai anh hop le (sai loai, sai duoi, qua nang
 * hoac noi dung khong dung dinh dang anh).
 */
public class InvalidImageException extends RuntimeException {

    public InvalidImageException(String message) {
        super(message);
    }
}