
package com.minkang.ultimate.menu;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class BoundItemListener implements Listener {
    private final MenuManager menu;
    public BoundItemListener(MenuManager menu){ this.menu = menu; }

    @EventHandler
    public void onUse(PlayerInteractEvent e){
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = e.getItem();
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Byte flag = pdc.get(new NamespacedKey(Main.get(), "menu_opener"), PersistentDataType.BYTE);
        if (flag != null && flag == (byte)1){
            e.setCancelled(true);
            Player p = e.getPlayer();
            menu.open(p);
        }
    }
}
