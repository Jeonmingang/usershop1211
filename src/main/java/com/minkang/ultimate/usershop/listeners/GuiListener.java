package com.minkang.ultimate.usershop.listeners;

import com.minkang.ultimate.usershop.gui.MainShopsGUI;
import com.minkang.ultimate.usershop.gui.PlayerShopGUI;
import com.minkang.ultimate.usershop.gui.SearchResultsGUI;
import com.minkang.ultimate.usershop.gui.StorageGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class GuiListener implements Listener {

    private final com.minkang.ultimate.usershop.Main plugin;
    public GuiListener(com.minkang.ultimate.usershop.Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory() == null) return;
        InventoryHolder holder = e.getInventory().getHolder();
        if (holder instanceof MainShopsGUI) {
            ((MainShopsGUI) holder).onClick(e);
        } else if (holder instanceof PlayerShopGUI) {
            ((PlayerShopGUI) holder).onClick(e);
        } else if (holder instanceof SearchResultsGUI) {
            ((SearchResultsGUI) holder).onClick(e);
        } else if (holder instanceof StorageGUI) {
            ((StorageGUI) holder).onClick(e);
        }
        // If not our GUI, do nothing (ensures /invsee compatibility).
    }
}
