package com.minkang.ultimate.usershop.gui;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.data.ShopManager;
import com.minkang.ultimate.usershop.model.Listing;
import com.minkang.ultimate.usershop.model.PlayerShop;
import com.minkang.ultimate.usershop.util.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerShopGUI implements InventoryHolder {

    private final Main plugin;
    private final Player viewer;
    private final UUID owner;
    private Inventory inv;
    private int page;

    public PlayerShopGUI(Main plugin, Player viewer, UUID owner) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.owner = owner;
    }

    public void open(int page) {
        this.page = page;
        int slots = plugin.getShopManager().getSlotCount(owner);
        int size = Math.min(Math.max(9, ((slots + 8) / 9) * 9), 54);
        String title = Main.color(plugin.getConfig().getString("settings.titles.player", "유저 상점 | {player}")
                .replace("{player}", Bukkit.getOfflinePlayer(owner).getName() == null ? "??" : Bukkit.getOfflinePlayer(owner).getName()));
        inv = Bukkit.createInventory(this, size, title);
        fill();
        viewer.openInventory(inv);
    }

    private void fill() {
        PlayerShop shop = plugin.getShopManager().getOrCreateShop(owner);
        for (Map.Entry<Integer, Listing> e : shop.getListings().entrySet()) {
            int slot = e.getKey();
            if (slot < 0 || slot >= inv.getSize()) continue;
            Listing l = e.getValue();
            ItemStack display = l.getItem().clone();
            ItemMeta meta = display.getItemMeta();
            String pretty = ItemUtils.getPrettyName(display);
            java.util.List<String> merged = new java.util.ArrayList<>();
            if (meta != null && meta.hasLore()) merged.addAll(meta.getLore());
            merged.addAll(makeLore(pretty, l.getPrice(), l.getStock(), Bukkit.getOfflinePlayer(owner)));
            meta.setLore(merged);
            display.setItemMeta(meta);
            inv.setItem(slot, display);
        }
        // back button
        int backSlot = plugin.getConfig().getInt("settings.icons.back.slot", 49);
        if (backSlot >= 0 && backSlot < inv.getSize()) {
            inv.setItem(backSlot, ItemUtils.iconFromCfg(plugin, "settings.icons.back"));
        }
    }

    private List<String> makeLore(String item, double price, int stock, OfflinePlayer op) {
        List<String> lore = new ArrayList<>();
        for (String l : plugin.getConfig().getStringList("format.player-shop-lore")) {
            lore.add(Main.color(l
                    .replace("{item}", item)
                    .replace("{price}", String.valueOf(price))
                    .replace("{stock}", String.valueOf(stock))
                    .replace("{seller}", op.getName()==null?op.getUniqueId().toString():op.getName())));
        }
        return lore;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public void onClick(InventoryClickEvent e) {
        e.setCancelled(true);
        int raw = e.getRawSlot();
        if (raw < 0 || raw >= inv.getSize()) return;

        int backSlot = plugin.getConfig().getInt("settings.icons.back.slot", 49);
        if (raw == backSlot) {
            new MainShopsGUI(plugin, viewer).open(0);
            return;
        }

        ItemStack clicked = inv.getItem(raw);
        if (clicked == null) return;

        boolean shift = e.getClick() == ClickType.SHIFT_LEFT;
        int buyAmount = shift ? 64 : 1;

        plugin.getShopManager().handlePurchase(viewer, owner, raw, buyAmount);
        viewer.playSound(viewer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.2f);
        // refresh
        open(page);
    }
}
