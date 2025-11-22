package com.minkang.ultimate.usershop.util;

import com.minkang.ultimate.usershop.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ItemUtils {

    public static ItemStack playerHead(OfflinePlayer op) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof SkullMeta) {
            SkullMeta sm = (SkullMeta) meta;
            sm.setOwningPlayer(op);
            head.setItemMeta(sm);
        }
        return head;
    }

    public static ItemStack iconFromCfg(Main plugin, String path) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection(path);
        if (sec == null) return new ItemStack(Material.BARRIER);
        String mat = sec.getString("material", "BARRIER");
        ItemStack it;
        try {
            it = new ItemStack(Material.valueOf(mat.toUpperCase(Locale.ROOT)));
        } catch (Exception ex) {
            it = new ItemStack(Material.BARRIER);
        }
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(Main.color(sec.getString("name", "")));
        List<String> lore = new ArrayList<>();
        for (String l : sec.getStringList("lore")) {
            lore.add(Main.color(l));
        }
        meta.setLore(lore);
        it.setItemMeta(meta);
        return it;
    }

    public static String getPrettyName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName().replace("§", "&");
        }
        // fallback to material name
        String mat = item.getType().name().toLowerCase(Locale.ROOT).replace("_", " ");
        return mat;
    }

    public static boolean giveItem(Player p, ItemStack item) {
        Inventory inv = p.getInventory();
        ItemStack toGive = item.clone();

        // Only proceed if the entire stack can be added without spilling (no world drops).
        if (!canFullyAdd(inv, toGive)) {
            return false;
        }
        inv.addItem(toGive);
        return true;
    }

    private static boolean canFullyAdd(Inventory inv, ItemStack item) {
        int remaining = item.getAmount();
        int max = item.getMaxStackSize();

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack cur = inv.getItem(i);
            if (cur == null || cur.getType() == org.bukkit.Material.AIR) {
                int fit = Math.min(max, remaining);
                remaining -= fit;
            } else if (cur.isSimilar(item)) {
                int space = Math.max(0, max - cur.getAmount());
                int fit = Math.min(space, remaining);
                remaining -= fit;
            }
            if (remaining <= 0) return true;
        }
        return remaining <= 0;
    }

    public static boolean isSimilarIgnoreAmount(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getType() != b.getType()) return false;
        if (a.hasItemMeta() != b.hasItemMeta()) return false;
        if (a.hasItemMeta()) {
            ItemMeta am = a.getItemMeta();
            ItemMeta bm = b.getItemMeta();
            String an = am.hasDisplayName() ? am.getDisplayName() : "";
            String bn = bm.hasDisplayName() ? bm.getDisplayName() : "";
            if (!an.equals(bn)) return false;
            List<String> al = am.getLore();
            List<String> bl = bm.getLore();
            if (al == null) al = new ArrayList<String>();
            if (bl == null) bl = new ArrayList<String>();
            if (al.size() != bl.size()) return false;
            for (int i=0;i<al.size();i++) {
                if (!al.get(i).equals(bl.get(i))) return false;
            }
        }
        return true;
    }

    public static boolean consumeOne(Player p, ItemStack target) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it == null) continue;
            if (isSimilarIgnoreAmount(it, target)) {
                int amt = it.getAmount();
                if (amt <= 1) inv.setItem(i, null);
                else {
                    ItemStack copy = it.clone();
                    copy.setAmount(amt - 1);
                    inv.setItem(i, copy);
                }
                return true;
            }
        }
        return false;
    }

    // Normalize Korean/Japanese/English strings for fuzzy search
    public static String normalize(String s) {
        String t = s==null ? "" : s;
        t = t.toLowerCase(Locale.ROOT);
        t = t.replace("§", "");
        t = t.replace("&", "");
        t = Normalizer.normalize(t, Normalizer.Form.NFKD);
        // basic strip spaces
        t = t.replace(" ", "");
        return t;
    }
}
