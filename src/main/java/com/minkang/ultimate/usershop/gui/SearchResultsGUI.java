package com.minkang.ultimate.usershop.gui;

import com.minkang.ultimate.usershop.Main;
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
import org.bukkit.Sound;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SearchResultsGUI implements InventoryHolder {

    private final Main plugin;
    private final Player viewer;
    private final List<Result> results = new ArrayList<>();
    private Inventory inv;
    private final String rawQuery;
    private int page;

    public static class Result {
        public UUID owner;
        public int slot;
        public Listing listing;
    }


    private void playClick(float pitch) {
        try {
            viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, pitch);
        } catch (Throwable ignored) {}
    }

    public SearchResultsGUI(Main plugin, Player viewer, String query) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.rawQuery = query;
        // build results
        java.util.Set<String> needles = new java.util.HashSet<>();
        ItemUtils.buildSearchNeedles(query, needles);
        String q = ItemUtils.normalize(query);
        // 1) translations.yml (포켓몬/기타 아이템 별칭)
        try {
            java.io.File f = new java.io.File(plugin.getDataFolder(), "translations.yml");
            if (f.exists()) {
                org.bukkit.configuration.file.YamlConfiguration y = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
                org.bukkit.configuration.ConfigurationSection aliases = y.getConfigurationSection("aliases");
                if (aliases != null) {
                    expandAliasesFromSection(aliases, q, needles);
                }
            }
        } catch (Exception ignore) {}

        // 2) vanilla-translations.yml (바닐라 아이템 한글 별칭)
        try {
            java.io.File f = new java.io.File(plugin.getDataFolder(), "vanilla-translations.yml");
            if (f.exists()) {
                org.bukkit.configuration.file.YamlConfiguration y = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
                org.bukkit.configuration.ConfigurationSection aliases = y.getConfigurationSection("aliases");
                if (aliases != null) {
                    expandAliasesFromSection(aliases, q, needles);
                }
            }
        } catch (Exception ignore) {}
        // ALIAS_EXPANSION

        for (PlayerShop ps : plugin.getShopManager().allShops()) {
            for (Map.Entry<Integer, Listing> e : ps.getListings().entrySet()) {
                Listing l = e.getValue();
                ItemStack stack = l.getItem();
                String baseName = ItemUtils.getPrettyName(stack);
                StringBuilder sb = new StringBuilder();
                if (baseName != null) sb.append(baseName);
                if (stack != null) {
                    sb.append(" ").append(stack.getType().name());
                    ItemMeta meta = stack.getItemMeta();
                    if (meta != null) {
                        if (meta.hasDisplayName()) {
                            sb.append(" ").append(meta.getDisplayName());
                        }
                        if (meta.hasLore()) {
                            for (String line : meta.getLore()) {
                                sb.append(" ").append(line);
                            }
                        }
                    }
                }
                // also append pretty translated name so that Korean aliases are searchable
                String prettyName = ItemUtils.getPrettyName(stack);
                if (prettyName != null && !prettyName.isEmpty()) {
                    sb.append(" ").append(prettyName);
                }
                String norm = ItemUtils.normalize(sb.toString());
                if (needles.stream().anyMatch(n -> norm.contains(n) || n.contains(norm))) {
                    Result r = new Result();
                    r.owner = ps.getOwner();
                    r.slot = e.getKey();
                    r.listing = l;
                    results.add(r);
                }
            }
        }
    }

    private void expandAliasesFromSection(org.bukkit.configuration.ConfigurationSection aliases, String q, java.util.Set<String> needles) {
        for (String key : aliases.getKeys(false)) {
            String keyN = ItemUtils.normalize(key);
            java.util.List<String> alts = aliases.getStringList(key);
            boolean hit = false;
            if (keyN.contains(q) || q.contains(keyN)) hit = true;
            for (String a : alts) {
                String aN = ItemUtils.normalize(a);
                if (aN.contains(q) || q.contains(aN)) hit = true;
            }
            if (hit) {
                needles.add(keyN);
                for (String a : alts) {
                    needles.add(ItemUtils.normalize(a));
                }
            }
        }
    }


    public void open(int page) {
        this.page = page;
        String title = Main.color(plugin.getConfig().getString("settings.titles.search", "검색 결과").replace("{query}", rawQuery));
        inv = Bukkit.createInventory(this, 54, title);
        fill(page);
        viewer.openInventory(inv);
    }

    private void fill(int page) {
        int start = page * 45;
        int end = Math.min(start + 45, results.size());
        int idx = 0;
        for (int i = start; i < end; i++) {
            Result r = results.get(i);
            int slot = idx++;
            ItemStack it = r.listing.getItem().clone();
            ItemMeta meta = it.getItemMeta();
            OfflinePlayer op = Bukkit.getOfflinePlayer(r.owner);
            String pretty = ItemUtils.getPrettyName(it);
            if (meta != null) {
                meta.setDisplayName(Main.color("&f" + pretty));
            }
            List<String> lore = new ArrayList<>();
            lore.add(Main.color(plugin.getConfig().getString("format.price", "가격: {price}").replace("{price}", String.valueOf(r.listing.getPrice()))));
            lore.add(Main.color(plugin.getConfig().getString("format.seller", "판매자: {seller}")
                    .replace("{seller}", op.getName()==null?op.getUniqueId().toString():op.getName())));
            lore.add(Main.color(plugin.getConfig().getString("format.stock", "재고: {stock}").replace("{stock}", String.valueOf(r.listing.getStock()))));
            meta.setLore(lore);
            it.setItemMeta(meta);
            inv.setItem(slot, it);
        }
        inv.setItem(plugin.getConfig().getInt("settings.icons.prev.slot",45), ItemUtils.iconFromCfg(plugin, "settings.icons.prev"));
        inv.setItem(plugin.getConfig().getInt("settings.icons.next.slot",53), ItemUtils.iconFromCfg(plugin, "settings.icons.next"));
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public void onClick(InventoryClickEvent e) {
        e.setCancelled(true);
        int raw = e.getRawSlot();
        if (raw < 0) return;
        int prevSlot = plugin.getConfig().getInt("settings.icons.prev.slot",45);
        int nextSlot = plugin.getConfig().getInt("settings.icons.next.slot",53);
        if (raw == prevSlot) {
            playClick(0.9f);
            open(Math.max(0, page - 1));
            return;
        }
        if (raw == nextSlot) {
            playClick(1.0f);
            open(page + 1);
            return;
        }
        int index = page * 45 + raw;
        if (index >= results.size()) return;
        Result r = results.get(index);
        // open that player's shop directly
        playClick(1.1f);
        new PlayerShopGUI(plugin, viewer, r.owner).open(0);
    }
}