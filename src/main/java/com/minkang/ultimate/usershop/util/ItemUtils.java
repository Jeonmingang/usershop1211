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
import java.util.HashMap;
import java.util.Map;

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

    // === 아이템 이름 한글/별칭 변환용 맵 ===
    private static final Map<String, String> TRANSLATION_MAP = new HashMap<>();

    /**
     * 플러그인 enable 시 호출해서 translations.yml / vanilla-translations.yml 의 aliases 섹션을
     * 전부 읽어와서 한글 기반 이름 매핑을 구성한다.
     *
     * key = 보여줄 한글 이름
     * value 목록 = 영어/한글/기타 별칭들
     *
     * normalize() 한 값을 키로 삼아서 어떤 형태로 검색/표시되어도 같은 한글 이름으로 통일한다.
     */
    public static void initTranslations(Main plugin) {
        TRANSLATION_MAP.clear();
        loadAliasesFile(plugin, "translations.yml");
        loadAliasesFile(plugin, "vanilla-translations.yml");
    }

    private static void loadAliasesFile(Main plugin, String fileName) {
        java.io.File f = new java.io.File(plugin.getDataFolder(), fileName);
        if (!f.exists()) return;
        org.bukkit.configuration.file.YamlConfiguration y;
        try {
            y = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
        } catch (Exception ex) {
            plugin.getLogger().warning("[UltimateUserShop] Failed to load aliases file " + fileName + ": " + ex.getMessage());
            return;
        }
        ConfigurationSection aliases = y.getConfigurationSection("aliases");
        if (aliases == null) return;

        for (String key : aliases.getKeys(false)) {
            String display = key; // 실제로 보여줄 한글 이름
            String keyNorm = normalize(display);
            if (!keyNorm.isEmpty()) {
                TRANSLATION_MAP.put(keyNorm, display);
            }
            for (String alt : aliases.getStringList(key)) {
                if (alt == null) continue;
                String n = normalize(alt);
                if (!n.isEmpty()) {
                    TRANSLATION_MAP.put(n, display);
                }
            }
        }
    }


    /**
     * 원래 이름을 받아서 translations 맵을 이용해 한글 이름으로 치환한다.
     * 매핑이 없으면 원래 문자열을 그대로 반환.
     */
    public static String translateName(String original) {
        if (original == null) return "";
        String norm = normalize(original);
        String mapped = TRANSLATION_MAP.get(norm);
        return mapped != null ? mapped : original;
    }

    /**
     * 검색어에 대해 사용할 탐색 문자열 집합을 구성한다.
     * - 원문(query)을 normalize 한 값
     * - translations 맵에서 해당 query와 관련된 항목들(표시 이름/별칭)의 normalize 값
     */
    public static void buildSearchNeedles(String query, java.util.Set<String> needles) {
        if (needles == null) return;
        String q = normalize(query);
        if (q != null && !q.isEmpty()) {
            needles.add(q);
        }
        // translations 맵과 연동
        for (Map.Entry<String, String> entry : TRANSLATION_MAP.entrySet()) {
            String altNorm = entry.getKey();         // 별칭(영문/한글 등) normalize 값
            String display = entry.getValue();      // 실제로 보여줄 한글 이름
            String dispNorm = normalize(display);
            boolean hit = false;
            if (!q.isEmpty()) {
                if (altNorm.contains(q) || q.contains(altNorm)) hit = true;
                if (dispNorm.contains(q) || q.contains(dispNorm)) hit = true;
            }
            if (hit) {
                needles.add(altNorm);
                needles.add(dispNorm);
            }
        }
    }

    /**
     * GUI 로어/검색 등에 사용할 "예쁜 이름" 생성
     * - 우선 아이템의 디스플레이 이름/재질 이름을 가져오고
     * - translations.yml / vanilla-translations.yml 에 등록된 별칭이 있으면 한글 이름으로 치환한다.
     */
    public static String getPrettyName(ItemStack item) {
        if (item == null) return "알 수 없는 아이템";

        String base;
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            // 색코드는 제거하고 내용만 사용해서 매핑
            base = meta.getDisplayName().replace("§", "").replace("&", "");
        } else {
            base = item.getType().name().toLowerCase(Locale.ROOT).replace("_", " ");
        }
        String translated = translateName(base);
        // 색코드는 여기서는 붙이지 않고, 호출하는 쪽(Main.color)에서 처리하게 둔다.
        return translated;
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