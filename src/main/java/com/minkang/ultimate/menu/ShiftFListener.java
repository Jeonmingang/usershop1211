
package com.minkang.ultimate.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class ShiftFListener implements Listener {
    private final MenuManager menu;
    public ShiftFListener(MenuManager menu){ this.menu = menu; }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e){
        Player p = e.getPlayer();
        if (p.isSneaking()){
            e.setCancelled(true);
            menu.open(p);
        }
    }
}
