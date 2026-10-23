package com.utetra.module.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cau hinh cong thanh toan (tuan 4).
 *
 * KHONG dat khoa that trong file nay va cung khong dat trong application.properties
 * (file dung chung, bi day len GitHub). Moi nguoi tu dien vao
 * application-local.properties (da nam trong .gitignore):
 *
 *   app.payment.vnpay.tmn-code=...
 *   app.payment.vnpay.hash-secret=...
 *   app.payment.momo.partner-code=...
 *   app.payment.momo.access-key=...
 *   app.payment.momo.secret-key=...
 *
 * Cac gia tri mac dinh con lai la thong tin cong khai cua sandbox (URL, ma lenh...).
 * Chua dien khoa thi module van chay: COD dung binh thuong, VNPAY/MOMO bao
 * "chua cau hinh" thay vi nem loi kho hieu.
 */
@Component
@ConfigurationProperties(prefix = "app.payment")
public class PaymentGatewayProperties {

    private final Vnpay vnpay = new Vnpay();
    private final Momo momo = new Momo();

    public Vnpay getVnpay() { return vnpay; }
    public Momo getMomo() { return momo; }

    public static class Vnpay {
        private String tmnCode = "";
        private String hashSecret = "";
        private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        private String returnUrl = "http://localhost:8080/payment/vnpay/return";
        private String ipnUrl = "http://localhost:8080/payment/vnpay/ipn";
        private String version = "2.1.0";
        private String command = "pay";
        private String orderType = "other";
        private String locale = "vn";
        private String currCode = "VND";
        /** So phut don thanh toan con hieu luc. */
        private int expireMinutes = 15;

        public boolean isConfigured() {
            return tmnCode != null && !tmnCode.isBlank()
                    && hashSecret != null && !hashSecret.isBlank();
        }

        public String getTmnCode() { return tmnCode; }
        public void setTmnCode(String tmnCode) { this.tmnCode = tmnCode; }
        public String getHashSecret() { return hashSecret; }
        public void setHashSecret(String hashSecret) { this.hashSecret = hashSecret; }
        public String getPayUrl() { return payUrl; }
        public void setPayUrl(String payUrl) { this.payUrl = payUrl; }
        public String getReturnUrl() { return returnUrl; }
        public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
        public String getIpnUrl() { return ipnUrl; }
        public void setIpnUrl(String ipnUrl) { this.ipnUrl = ipnUrl; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        public String getOrderType() { return orderType; }
        public void setOrderType(String orderType) { this.orderType = orderType; }
        public String getLocale() { return locale; }
        public void setLocale(String locale) { this.locale = locale; }
        public String getCurrCode() { return currCode; }
        public void setCurrCode(String currCode) { this.currCode = currCode; }
        public int getExpireMinutes() { return expireMinutes; }
        public void setExpireMinutes(int expireMinutes) { this.expireMinutes = expireMinutes; }
    }

    public static class Momo {
        private String partnerCode = "";
        private String accessKey = "";
        private String secretKey = "";
        private String endpoint = "https://test-payment.momo.vn/v2/gateway/api/create";
        private String returnUrl = "http://localhost:8080/payment/momo/return";
        private String ipnUrl = "http://localhost:8080/payment/momo/ipn";
        private String requestType = "captureWallet";
        private String lang = "vi";

        public boolean isConfigured() {
            return partnerCode != null && !partnerCode.isBlank()
                    && accessKey != null && !accessKey.isBlank()
                    && secretKey != null && !secretKey.isBlank();
        }

        public String getPartnerCode() { return partnerCode; }
        public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getReturnUrl() { return returnUrl; }
        public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
        public String getIpnUrl() { return ipnUrl; }
        public void setIpnUrl(String ipnUrl) { this.ipnUrl = ipnUrl; }
        public String getRequestType() { return requestType; }
        public void setRequestType(String requestType) { this.requestType = requestType; }
        public String getLang() { return lang; }
        public void setLang(String lang) { this.lang = lang; }
    }
}