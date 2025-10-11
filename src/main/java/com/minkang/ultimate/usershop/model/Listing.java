package com.minkang.ultimate.usershop.model;

import org.bukkit.inventory.ItemStack;

public class Listing {

    private ItemStack item;
    private double price;
    private int stock;
    private long createdAt; // epoch millis

    public Listing(ItemStack item, double price, int stock) {
        this(item, price, stock, System.currentTimeMillis());
    }

    public Listing(ItemStack item, double price, int stock, long createdAt) {
        this.item = item;
        this.price = price;
        this.stock = stock;
        this.createdAt = createdAt;
    }

    public ItemStack getItem() { return item; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long t) { this.createdAt = t; }
}
