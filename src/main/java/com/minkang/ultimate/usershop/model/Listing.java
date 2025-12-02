package com.minkang.ultimate.usershop.model;

import org.bukkit.inventory.ItemStack;

public class Listing {

    private ItemStack item;
    private double price;
    private int stock;
    private long createdAt; // epoch millis
    // 자동 재등록 횟수 추적용
    private int relistCount;

    public Listing(ItemStack item, double price, int stock) {
        this(item, price, stock, System.currentTimeMillis(), 0);
    }

    public Listing(ItemStack item, double price, int stock, long createdAt) {
        this(item, price, stock, createdAt, 0);
    }

    public Listing(ItemStack item, double price, int stock, long createdAt, int relistCount) {
        this.item = item;
        this.price = price;
        this.stock = stock;
        this.createdAt = createdAt;
        this.relistCount = relistCount;
    }

    public ItemStack getItem() { return item; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long t) { this.createdAt = t; }
    public int getRelistCount() { return relistCount; }
    public void setRelistCount(int relistCount) { this.relistCount = relistCount; }
}
