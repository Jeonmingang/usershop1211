package com.minkang.ultimate.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class MenuClickListener implements Listener {
    private final MenuManager menu;

    public MenuClickListener(MenuManager menu){
        this.menu = menu;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e){
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        Inventory inv = e.getInventory();
        if (!menu.isMenuInventory(inv)) return;

        e.setCancelled(true); // 꺼내기 차단
        int rawSlot = e.getRawSlot();
        if (rawSlot < 0 || rawSlot >= inv.getSize()) return; // 상단 인벤토리만

        menu.handleClick(p, inv, rawSlot);
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent e){
        if (!(e.getWhoClicked() instanceof Player)) return;
        Inventory inv = e.getInventory();
        if (!menu.isMenuInventory(inv)) return;
        e.setCancelled(true);
    }
}