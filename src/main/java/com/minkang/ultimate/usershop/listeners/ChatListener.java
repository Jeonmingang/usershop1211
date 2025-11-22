package com.minkang.ultimate.usershop.listeners;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.gui.SearchResultsGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final Main plugin;
    public ChatListener(Main plugin) { this.plugin = plugin; }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!plugin.getShopManager().isWaitingSearch(p.getUniqueId())) return;
        e.setCancelled(true);
        plugin.getShopManager().setWaitingSearch(p.getUniqueId(), false);
        String msg = e.getMessage();
        if (msg.equalsIgnoreCase("취소")) {
            p.sendMessage(plugin.msg("search-cancelled"));
            return;
        }
        // open search GUI on main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            new SearchResultsGUI(plugin, p, msg).open(0);
        });
    }
}
