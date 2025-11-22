package com.minkang.ultimate.usershop.gui;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.data.ShopManager;
import com.minkang.ultimate.usershop.model.Listing;
import com.minkang.ultimate.usershop.model.PlayerShop;
import com.minkang.ultimate.usershop.util.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainShopsGUI implements InventoryHolder {

    private final Main plugin;
    private final Player viewer;
    private Inventory inv;
    private int page;
    private final List<Ref> refs = new ArrayList<>();

    private static class Ref {
        UUID owner;
        int slot;
        Listing listing;
    }

    public MainShopsGUI(Main plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    public void open(int page) {
        this.page = page;
        String title = Main.color(plugin.getConfig().getString("settings.titles.main", "유저 상점 | 전체"));
        inv = Bukkit.createInventory(this, 54, title);
        fill();
        viewer.openInventory(inv);
    }

    private String leftTime(long createdAt) {
        int days = plugin.getConfig().getInt("expiry.days", 5);
        long ttl = days * 24L*60L*60L*1000L;
        long left = createdAt + ttl - System.currentTimeMillis();
        if (left < 0) left = 0;
        long d = left / (24L*60L*60L*1000L);
        long h = (left / (60L*60L*1000L)) % 24;
        long m = (left / (60L*1000L)) % 60;
        return d + "일 " + h + "시간 " + m + "분";
    }

    private void fill() {
        refs.clear();
        // collect all listings
        List<PlayerShop> shops = new ArrayList<>(plugin.getShopManager().allShops());
        for (PlayerShop ps : shops) {
            for (Map.Entry<Integer, Listing> e : ps.getListings().entrySet()) {
                Ref r = new Ref();
                r.owner = ps.getOwner();
                r.slot = e.getKey();
                r.listing = e.getValue();
                refs.add(r);
            }
        }
        // sort by createdAt desc (most recent first)
        refs.sort((a,b)-> Long.compare(b.listing.getCreatedAt(), a.listing.getCreatedAt()));

        int start = page * 45;
        int end = Math.min(start + 45, refs.size());
        int idx = 0;
        for (int i = start; i < end; i++) {
            Ref r = refs.get(i);
            int guiSlot = idx++;
            ItemStack it = r.listing.getItem().clone();
            ItemMeta meta = it.getItemMeta();
            OfflinePlayer op = Bukkit.getOfflinePlayer(r.owner);
            List<String> lore = new ArrayList<>();
            if (meta != null && meta.hasLore()) lore.addAll(meta.getLore());
            lore.add(Main.color(plugin.getConfig().getString("format.price", "&6가격: &e{price}")
                    .replace("{price}", String.valueOf(r.listing.getPrice()))));
            lore.add(Main.color(plugin.getConfig().getString("format.seller", "&7판매자: &f{seller}")
                    .replace("{seller}", op.getName()==null?op.getUniqueId().toString():op.getName())));
            lore.add(Main.color("&7남은시간: &f" + leftTime(r.listing.getCreatedAt())));
            meta.setLore(lore);

            // recent mark
            int recentH = plugin.getConfig().getInt("expiry.recent-hours", 24);
            if (System.currentTimeMillis() - r.listing.getCreatedAt() <= recentH * 3600L * 1000L) {
                String dn = meta.hasDisplayName() ? meta.getDisplayName() : ItemUtils.getPrettyName(it);
                meta.setDisplayName(Main.color("&a[NEW] &r" + dn));
            }
            it.setItemMeta(meta);
            inv.setItem(guiSlot, it);
        }

        // controls
        inv.setItem(plugin.getConfig().getInt("settings.icons.prev.slot",45), ItemUtils.iconFromCfg(plugin, "settings.icons.prev"));
        inv.setItem(plugin.getConfig().getInt("settings.icons.search.slot",49), ItemUtils.iconFromCfg(plugin, "settings.icons.search"));
        inv.setItem(plugin.getConfig().getInt("settings.icons.next.slot",53), ItemUtils.iconFromCfg(plugin, "settings.icons.next"));
        int mySlot = plugin.getConfig().getInt("settings.icons.myshop.slot",48);
        if (mySlot >= 0 && mySlot < 54) {
            inv.setItem(mySlot, ItemUtils.iconFromCfg(plugin, "settings.icons.myshop"));
        }
    }

    @Override
    public Inventory getInventory() { return inv; }

    public void onClick(InventoryClickEvent e) {
        e.setCancelled(true);
        int raw = e.getRawSlot();
        if (raw < 0) return;
        int prevSlot = plugin.getConfig().getInt("settings.icons.prev.slot",45);
        int searchSlot = plugin.getConfig().getInt("settings.icons.search.slot",49);
        int nextSlot = plugin.getConfig().getInt("settings.icons.next.slot",53);
        int mySlot = plugin.getConfig().getInt("settings.icons.myshop.slot",48);

        if (raw < 45) {
            int index = page * 45 + raw;
            if (index >= refs.size()) return;
            Ref r = refs.get(index);
            new PlayerShopGUI(plugin, viewer, r.owner).open(0);
            return;
        }
        if (raw == prevSlot) {
            open(Math.max(0, page - 1));
        } else if (raw == searchSlot) {
            viewer.closeInventory();
            viewer.sendMessage(plugin.msg("search-type"));
            plugin.getShopManager().setWaitingSearch(viewer.getUniqueId(), true);
        } else if (raw == nextSlot) {
            open(page + 1);
        } else if (raw == mySlot) {
            new PlayerShopGUI(plugin, viewer, viewer.getUniqueId()).open(0);
        }
    }
}
