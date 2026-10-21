package com.utetra.module.cart.dto;

import java.util.ArrayList;
import java.util.List;

public class AddToCartRequest {

    private Long productId;
    private Long variantId;
    private List<Long> toppingIds = new ArrayList<>();
    private int quantity = 1;
    private int sugarLevel = 100;
    private int iceLevel = 100;
    private String note;
    private boolean buyNow = false;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }
    public List<Long> getToppingIds() { return toppingIds; }
    public void setToppingIds(List<Long> toppingIds) { this.toppingIds = toppingIds; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getSugarLevel() { return sugarLevel; }
    public void setSugarLevel(int sugarLevel) { this.sugarLevel = sugarLevel; }
    public int getIceLevel() { return iceLevel; }
    public void setIceLevel(int iceLevel) { this.iceLevel = iceLevel; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public boolean isBuyNow() { return buyNow; }
    public void setBuyNow(boolean buyNow) { this.buyNow = buyNow; }
}
