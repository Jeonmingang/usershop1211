package com.minkang.ultimate.usershop.listeners;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.gui.MainShopsGUI;
import com.minkang.ultimate.usershop.util.ItemSerializer;
import com.minkang.ultimate.usershop.util.ItemUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class InteractListener implements Listener {

    private final Main plugin;
    public InteractListener(Main plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        String b64 = plugin.getConfig().getString("items.open-item", "");
        if (b64 == null || b64.isEmpty()) return;
        ItemStack open = ItemSerializer.deserializeFromBase64(b64);
        if (open == null) return;
        if (ItemUtils.isSimilarIgnoreAmount(hand, open)) {
            e.setCancelled(true);
            new MainShopsGUI(plugin, p).open(0);
        }
    }
}
