package com.minkang.ultimate.usershop.gui;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.data.ShopManager;
import com.minkang.ultimate.usershop.model.Listing;
import com.minkang.ultimate.usershop.model.PlayerShop;
import com.minkang.ultimate.usershop.util.ItemUtils;
import com.minkang.ultimate.usershop.util.PokemonDataParser;
import com.minkang.ultimate.usershop.util.PokemonDataParser.PokemonSummary;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.enchantments.Enchantment;

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
    private final java.util.List<GroupRef> groupRefs = new java.util.ArrayList<>();
    private SortMode sortMode = SortMode.NEWEST;
    private FilterMode filterMode = FilterMode.ALL;
    private boolean groupSameItems = false;

    private static class Ref {
        UUID owner;
        int slot;
        Listing listing;
    }

    private static class GroupRef {
        String key;
        String displayName;
        ItemStack representative;
        int count;
        double minPrice;
        double maxPrice;
        double avgPrice;
        double totalPrice;
        long newestCreatedAt;
        long oldestCreatedAt;
    }

    private enum SortMode {
        NEWEST,
        OLDEST,
        PRICE_ASC,
        PRICE_DESC
    }

    private enum FilterMode {
        ALL,
        POKEMON_ONLY,
        NORMAL_ONLY
    }


    private void playClick(float pitch) {
        try {
            viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, pitch);
        } catch (Throwable ignored) {}
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


    private boolean isPokemonListing(Listing listing) {
        if (listing == null) return false;
        ItemStack item = listing.getItem();
        if (item == null) return false;
        PokemonSummary summary = PokemonDataParser.parse(item);
        return summary != null && summary.speciesName != null && !summary.speciesName.isEmpty();
    }

    private ItemStack buildSortIcon() {
        ItemStack it = new ItemStack(org.bukkit.Material.COMPASS);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Main.color("&e정렬 모드"));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add(Main.color("&7현재: &f" + getSortLabel()));
            lore.add(Main.color("&7클릭 시 정렬 순서를 변경합니다."));
            meta.setLore(lore);
            // 반짝이 효과 (항상 표시)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            it.setItemMeta(meta);
        }
        try {
            it.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
        } catch (Throwable ignored) {}
        return it;
    }

    private String getSortLabel() {
        switch (sortMode) {
            case NEWEST: return "최신 등록 순";
            case OLDEST: return "오래된 등록 순";
            case PRICE_ASC: return "가격 낮은 순";
            case PRICE_DESC: return "가격 높은 순";
            default: return "";
        }
    }

    private ItemStack buildFilterIcon() {
        ItemStack it = new ItemStack(org.bukkit.Material.HOPPER);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Main.color("&e필터"));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add(Main.color("&7현재: &f" + getFilterLabel()));
            lore.add(Main.color("&7클릭 시 포켓몬/일반 아이템을 전환합니다."));
            meta.setLore(lore);
            if (filterMode != FilterMode.ALL) {
                // 필터가 활성화된 상태일 때만 반짝이
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            it.setItemMeta(meta);
        }
        if (filterMode != FilterMode.ALL) {
            try {
                it.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
            } catch (Throwable ignored) {}
        }
        return it;
    }

    private String getFilterLabel() {
        switch (filterMode) {
            case ALL: return "전체 보기";
            case POKEMON_ONLY: return "포켓몬만 보기";
            case NORMAL_ONLY: return "일반 아이템만 보기";
            default: return "";
        }
    }

    private ItemStack buildGroupIcon() {
        ItemStack it = new ItemStack(org.bukkit.Material.CHEST);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Main.color("&e같은 아이템 묶기"));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add(Main.color("&7현재: &f" + getGroupLabel()));
            lore.add(Main.color("&7클릭 시 같은 아이템을 하나로 묶어 보여줍니다."));
            meta.setLore(lore);
            if (groupSameItems) {
                // 묶기 기능이 켜져 있을 때만 반짝이
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            it.setItemMeta(meta);
        }
        if (groupSameItems) {
            try {
                it.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
            } catch (Throwable ignored) {}
        }
        return it;
    }

    private String getGroupLabel() {
        return groupSameItems ? "켜짐" : "꺼짐";
    }

    
    private void fill() {
        refs.clear();
        groupRefs.clear();

        // collect all listings
        java.util.List<PlayerShop> shops = new java.util.ArrayList<>(plugin.getShopManager().allShops());
        java.util.List<Ref> all = new java.util.ArrayList<>();
        for (PlayerShop ps : shops) {
            for (java.util.Map.Entry<Integer, Listing> e : ps.getListings().entrySet()) {
                Ref r = new Ref();
                r.owner = ps.getOwner();
                r.slot = e.getKey();
                r.listing = e.getValue();
                all.add(r);
            }
        }

        // filter by pokemon / normal
        java.util.List<Ref> filtered = new java.util.ArrayList<>();
        for (Ref r : all) {
            switch (filterMode) {
                case ALL:
                    filtered.add(r);
                    break;
                case POKEMON_ONLY:
                    if (isPokemonListing(r.listing)) filtered.add(r);
                    break;
                case NORMAL_ONLY:
                    if (!isPokemonListing(r.listing)) filtered.add(r);
                    break;
            }
        }

        if (!groupSameItems) {
            // flat list mode
            refs.addAll(filtered);
            // sort
            switch (sortMode) {
                case NEWEST:
                    refs.sort((a, b) -> Long.compare(b.listing.getCreatedAt(), a.listing.getCreatedAt()));
                    break;
                case OLDEST:
                    refs.sort((a, b) -> Long.compare(a.listing.getCreatedAt(), b.listing.getCreatedAt()));
                    break;
                case PRICE_ASC:
                    refs.sort((a, b) -> Double.compare(a.listing.getPrice(), b.listing.getPrice()));
                    break;
                case PRICE_DESC:
                    refs.sort((a, b) -> Double.compare(b.listing.getPrice(), a.listing.getPrice()));
                    break;
            }

            int startIdx = page * 45;
            int endIdx = Math.min(startIdx + 45, refs.size());
            int idx = 0;
            for (int i = startIdx; i < endIdx; i++) {
                Ref r = refs.get(i);
                int guiSlot = idx++;
                ItemStack it = r.listing.getItem().clone();
                ItemMeta meta = it.getItemMeta();
                OfflinePlayer op = Bukkit.getOfflinePlayer(r.owner);
                java.util.List<String> lore = new java.util.ArrayList<>();
                if (meta != null && meta.hasLore()) lore.addAll(meta.getLore());
                lore.add(Main.color(plugin.getConfig().getString("format.price", "&6가격: &e{price}")
                        .replace("{price}", String.valueOf(r.listing.getPrice()))));

                // 전국 평균 시세/최저가 표시
                ShopManager.PriceStats stats = plugin.getShopManager().computePriceStats(it);
                if (stats != null && stats.count > 1) {
                    String tmpl = plugin.getConfig().getString("format.price-stats", "&7전국 평균: &f{avg} &7(최저: &f{min}&7 / 등록수: &f{count}&7)");
                    lore.add(Main.color(tmpl
                            .replace("{avg}", String.valueOf(stats.avg))
                            .replace("{min}", String.valueOf(stats.min))
                            .replace("{max}", String.valueOf(stats.max))
                            .replace("{count}", String.valueOf(stats.count))));
                }

                lore.add(Main.color(plugin.getConfig().getString("format.seller", "&7판매자: &f{seller}")
                        .replace("{seller}", op.getName() == null ? op.getUniqueId().toString() : op.getName())));
                lore.add(Main.color("&7남은시간: &f" + leftTime(r.listing.getCreatedAt())));
                if (meta != null) {
                    meta.setLore(lore);
                    // recent mark
                    int recentH = plugin.getConfig().getInt("expiry.recent-hours", 24);
                    if (System.currentTimeMillis() - r.listing.getCreatedAt() <= recentH * 3600L * 1000L) {
                        String dn = ItemUtils.getPrettyName(it);
                        meta.setDisplayName(Main.color("&a[NEW] &r" + dn));
                    }
                    it.setItemMeta(meta);
                }
                inv.setItem(guiSlot, it);
            }
        } else {
            // grouped mode: same item name merged
            java.util.Map<String, GroupRef> map = new java.util.LinkedHashMap<>();
            for (Ref r : filtered) {
                ItemStack item = r.listing.getItem();
                String baseName = ItemUtils.getPrettyName(item);
                String key = ItemUtils.normalize(baseName);
                GroupRef g = map.get(key);
                if (g == null) {
                    g = new GroupRef();
                    g.key = key;
                    g.displayName = baseName;
                    g.representative = item.clone();
                    g.count = 0;
                    g.totalPrice = 0.0;
                    g.minPrice = r.listing.getPrice();
                    g.maxPrice = r.listing.getPrice();
                    g.newestCreatedAt = r.listing.getCreatedAt();
                    g.oldestCreatedAt = r.listing.getCreatedAt();
                    map.put(key, g);
                }
                g.count++;
                g.totalPrice += r.listing.getPrice();
                if (r.listing.getPrice() < g.minPrice) g.minPrice = r.listing.getPrice();
                if (r.listing.getPrice() > g.maxPrice) g.maxPrice = r.listing.getPrice();
                if (r.listing.getCreatedAt() > g.newestCreatedAt) g.newestCreatedAt = r.listing.getCreatedAt();
                if (r.listing.getCreatedAt() < g.oldestCreatedAt) g.oldestCreatedAt = r.listing.getCreatedAt();
            }

            groupRefs.addAll(map.values());
            for (GroupRef g : groupRefs) {
                g.avgPrice = g.count > 0 ? g.totalPrice / g.count : 0.0;
            }

            // sort grouped by mode (use minPrice for price-based)
            switch (sortMode) {
                case NEWEST:
                    groupRefs.sort((a, b) -> Long.compare(b.newestCreatedAt, a.newestCreatedAt));
                    break;
                case OLDEST:
                    groupRefs.sort((a, b) -> Long.compare(a.oldestCreatedAt, b.oldestCreatedAt));
                    break;
                case PRICE_ASC:
                    groupRefs.sort((a, b) -> Double.compare(a.minPrice, b.minPrice));
                    break;
                case PRICE_DESC:
                    groupRefs.sort((a, b) -> Double.compare(b.minPrice, a.minPrice));
                    break;
            }

            int startIdx = page * 45;
            int endIdx = Math.min(startIdx + 45, groupRefs.size());
            int idx = 0;
            for (int i = startIdx; i < endIdx; i++) {
                GroupRef g = groupRefs.get(i);
                int guiSlot = idx++;
                ItemStack it = g.representative.clone();
                ItemMeta meta = it.getItemMeta();
                java.util.List<String> lore = new java.util.ArrayList<>();
                if (meta != null && meta.hasLore()) {
                    lore.addAll(meta.getLore());
                }
                lore.add(Main.color("&6최저가: &e" + g.minPrice + " &7/ 평균: &e" + String.format(java.util.Locale.ROOT, "%.2f", g.avgPrice)));
                lore.add(Main.color("&7등록 개수: &f" + g.count));
                if (meta != null) {
                    meta.setLore(lore);
                    String dn = meta.hasDisplayName() ? meta.getDisplayName() : ItemUtils.getPrettyName(it);
                    meta.setDisplayName(Main.color("&b[묶음] &r" + dn));
                    it.setItemMeta(meta);
                }
                inv.setItem(guiSlot, it);
            }
        }

        // controls
        int prevSlot = plugin.getConfig().getInt("settings.icons.prev.slot",45);
        int searchSlot = plugin.getConfig().getInt("settings.icons.search.slot",49);
        int nextSlot = plugin.getConfig().getInt("settings.icons.next.slot",53);
        int mySlot = plugin.getConfig().getInt("settings.icons.myshop.slot",48);

        inv.setItem(prevSlot, ItemUtils.iconFromCfg(plugin, "settings.icons.prev"));
        inv.setItem(searchSlot, ItemUtils.iconFromCfg(plugin, "settings.icons.search"));
        inv.setItem(nextSlot, ItemUtils.iconFromCfg(plugin, "settings.icons.next"));
        if (mySlot >= 0 && mySlot < 54) {
            inv.setItem(mySlot, ItemUtils.iconFromCfg(plugin, "settings.icons.myshop"));
        }

        int sortSlot = plugin.getConfig().getInt("settings.icons.sort.slot",46);
        if (sortSlot >= 0 && sortSlot < 54) {
            inv.setItem(sortSlot, buildSortIcon());
        }
        int filterSlot = plugin.getConfig().getInt("settings.icons.filter.slot",47);
        if (filterSlot >= 0 && filterSlot < 54) {
            inv.setItem(filterSlot, buildFilterIcon());
        }
        int groupSlot = plugin.getConfig().getInt("settings.icons.group.slot",50);
        if (groupSlot >= 0 && groupSlot < 54) {
            inv.setItem(groupSlot, buildGroupIcon());
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
        int sortSlot = plugin.getConfig().getInt("settings.icons.sort.slot",46);
        int filterSlot = plugin.getConfig().getInt("settings.icons.filter.slot",47);
        int groupSlot = plugin.getConfig().getInt("settings.icons.group.slot",50);

        if (raw < 45) {
            int index = page * 45 + raw;
            if (!groupSameItems) {
                if (index >= refs.size()) return;
                Ref r = refs.get(index);
                playClick(1.1f);
                new PlayerShopGUI(plugin, viewer, r.owner).open(0);
                return;
            } else {
                if (index >= groupRefs.size()) return;
                GroupRef g = groupRefs.get(index);
                playClick(1.05f);
                viewer.closeInventory();
                viewer.sendMessage(Main.color("&e[유저상점] &f'" + g.displayName + "' &7검색 결과를 엽니다."));
                new SearchResultsGUI(plugin, viewer, g.displayName).open(0);
                return;
            }
        }

        if (raw == prevSlot) {
            playClick(0.9f);
            open(Math.max(0, page - 1));
        } else if (raw == searchSlot) {
            playClick(1.0f);
            viewer.closeInventory();
            viewer.sendMessage(plugin.msg("search-type"));
            plugin.getShopManager().setWaitingSearch(viewer.getUniqueId(), true);
        } else if (raw == nextSlot) {
            playClick(1.0f);
            open(page + 1);
        } else if (raw == mySlot) {
            playClick(1.05f);
            new PlayerShopGUI(plugin, viewer, viewer.getUniqueId()).open(0);
        } else if (raw == sortSlot) {
            playClick(1.1f);
            switch (sortMode) {
                case NEWEST:
                    sortMode = SortMode.PRICE_ASC;
                    break;
                case PRICE_ASC:
                    sortMode = SortMode.PRICE_DESC;
                    break;
                case PRICE_DESC:
                    sortMode = SortMode.OLDEST;
                    break;
                case OLDEST:
                default:
                    sortMode = SortMode.NEWEST;
                    break;
            }
            open(0);
        } else if (raw == filterSlot) {
            playClick(1.1f);
            switch (filterMode) {
                case ALL:
                    filterMode = FilterMode.POKEMON_ONLY;
                    break;
                case POKEMON_ONLY:
                    filterMode = FilterMode.NORMAL_ONLY;
                    break;
                case NORMAL_ONLY:
                default:
                    filterMode = FilterMode.ALL;
                    break;
            }
            open(0);
        } else if (raw == groupSlot) {
            playClick(1.15f);
            groupSameItems = !groupSameItems;
            open(0);
        }
    }
}
