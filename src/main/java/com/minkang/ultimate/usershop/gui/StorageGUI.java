package com.minkang.ultimate.usershop.gui;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.util.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class StorageGUI implements InventoryHolder {

    private final Main plugin;
    private final Player viewer;
    private Inventory inv;
    private static final int COLLECT_SLOT = 49;


    private void playClick(float pitch) {
        try {
            viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, pitch);
        } catch (Throwable ignored) {}
    }

    public StorageGUI(Main plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    public void open() {
        String title = Main.color("&e유저 상점 &7| &f보관함");
        inv = Bukkit.createInventory(this, 54, title);
        fill();
        viewer.openInventory(inv);
    }

    private void fill() {
        java.util.List<ItemStack> items = plugin.getShopManager().getStorage(viewer.getUniqueId());
        // fill item showcase (0..44)
        int i = 0;
        for (ItemStack it : items) {
            if (i >= 45) break;
            inv.setItem(i++, it);
        }
        // collect button
        org.bukkit.inventory.ItemStack btn = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LIME_CONCRETE);
        org.bukkit.inventory.meta.ItemMeta im = btn.getItemMeta();
        if (im != null) {
            im.setDisplayName(Main.color("&a보관함 수령"));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add(Main.color("&7보관함의 아이템을 인벤토리로 받습니다."));
            lore.add(Main.color("&7보관함에 남은 아이템: &f" + items.size() + "개"));
            im.setLore(lore);
            // 수령 버튼 반짝이 효과
            im.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            btn.setItemMeta(im);
        }
        try {
            btn.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
        } catch (Throwable ignored) {}
        inv.setItem(COLLECT_SLOT, btn);
    }

    @Override
    public Inventory getInventory() { return inv; }

    public void onClick(InventoryClickEvent e) {
        // All clicks are internal to GUI
        e.setCancelled(true);
        int raw = e.getRawSlot();
        if (raw < 0 || raw >= inv.getSize()) return;

        // only handle collect button; block taking individual items
        if (raw == COLLECT_SLOT) {
            playClick(1.0f);
            java.util.List<ItemStack> items = new java.util.ArrayList<>(plugin.getShopManager().getStorage(viewer.getUniqueId()));
            if (items.isEmpty()) {
                viewer.sendMessage(Main.getInstance().msg("storage-empty"));
                return;
            }
            int received = 0;
            java.util.Iterator<ItemStack> it = items.iterator();
            while (it.hasNext()) {
                ItemStack stack = it.next().clone();
                if (ItemUtils.giveItem(viewer, stack)) {
                    // remove exactly that amount from storage
                    plugin.getShopManager().removeFromStorage(viewer.getUniqueId(), stack);
                    received += stack.getAmount();
                } else {
                    // inventory full; stop trying
                    viewer.sendMessage(Main.getInstance().msg("storage-inventory-full"));
                    break;
                }
            }
            if (received > 0) {
                viewer.sendMessage(Main.getInstance().msg("storage-collected").replace("{count}", String.valueOf(received)));
            }
            // refresh
            fill();
        } else {
            // Ignore item clicks (must use collect button)
        }
    }
}
