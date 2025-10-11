package com.minkang.ultimate.usershop.model;

import org.bukkit.inventory.ItemStack;

public class StoredItem {
    private ItemStack item;
    private String reason;
    private long storedAt;

    public StoredItem(ItemStack item, String reason, long storedAt) {
        this.item = item;
        this.reason = reason;
        this.storedAt = storedAt;
    }

    public ItemStack getItem() { return item; }
    public String getReason() { return reason; }
    public long getStoredAt() { return storedAt; }
}
