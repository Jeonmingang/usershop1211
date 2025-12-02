package com.minkang.ultimate.usershop.data;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.model.Listing;
import com.minkang.ultimate.usershop.model.PlayerShop;
import com.minkang.ultimate.usershop.util.ItemUtils;
import com.minkang.ultimate.usershop.util.DiscordWebhook;
import com.minkang.ultimate.usershop.util.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ShopManager {
    private final java.util.Map<java.util.UUID, java.util.List<org.bukkit.inventory.ItemStack>> storage = new java.util.concurrent.ConcurrentHashMap<>();

    private final Main plugin;
    private final File dataDir;
    
    private final java.io.File storageFile;
    private final java.io.File favoritesFile;
    // 관심 상품 키워드 (즐겨찾기) 저장용
    private final java.util.Map<java.util.UUID, java.util.List<String>> favorites = new java.util.concurrent.ConcurrentHashMap<>();
    private boolean favoritesLoaded = false;
private final Map<UUID, PlayerShop> shops = new ConcurrentHashMap<>();
    private final Set<UUID> searchWaiting = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());


    // === 일일 판매 집계 (디스코드 요약용) ===
    private static class DailySaleStats {
        double minPrice;
        double maxPrice;
        double totalPrice;
        int totalTrades;
        int totalAmount;
        String displayName;
    }

    private final java.util.Map<String, DailySaleStats> dailySales = new java.util.HashMap<>();
    private final ZoneId salesZoneId = ZoneId.systemDefault();
    private LocalDate salesDate = LocalDate.now(salesZoneId);
    private LocalDate lastSummaryDate = null;

    public ShopManager(Main plugin) {
        this.plugin = plugin;
        this.dataDir = new File(plugin.getDataFolder(), "shops");
        if (!dataDir.exists()) dataDir.mkdirs();
            this.storageFile = new java.io.File(plugin.getDataFolder(), "storage.yml");
            this.favoritesFile = new java.io.File(plugin.getDataFolder(), "favorites.yml");
}

    public void loadAll() {
        loadStorage();
        File[] files = dataDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
                UUID id = UUID.fromString(yml.getString("uuid"));
                PlayerShop ps = PlayerShop.fromYaml(yml);
                shops.put(id, ps);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to load shop file: " + f.getName() + " - " + ex.getMessage());
            }
        }
    }

    
    private void loadStorage() {
        try {
            if (storageFile.exists()) {
                org.bukkit.configuration.file.YamlConfiguration yml =
                        org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(storageFile);
                this.storage.clear();
                for (String key : yml.getKeys(false)) {
                    try {
                        java.util.UUID id = java.util.UUID.fromString(key);
                        java.util.List<org.bukkit.inventory.ItemStack> lst = new java.util.ArrayList<>();
                        java.util.List<?> raw = yml.getList(key);
                        if (raw != null && !raw.isEmpty()) {
                            Object first = raw.get(0);
                            if (first instanceof org.bukkit.inventory.ItemStack) {
                                // legacy format: direct ItemStack list
                                for (Object o : raw) {
                                    if (o instanceof org.bukkit.inventory.ItemStack) {
                                        lst.add(((org.bukkit.inventory.ItemStack) o).clone());
                                    }
                                }
                            } else if (first instanceof String) {
                                // new format: base64 strings
                                for (Object o : raw) {
                                    String s = (String) o;
                                    org.bukkit.inventory.ItemStack it = com.minkang.ultimate.usershop.util.ItemSerializer.deserializeFromBase64(s);
                                    if (it != null) lst.add(it);
                                }
                            } else {
                                // safety: try per-index retrieval
                                org.bukkit.configuration.ConfigurationSection sec = yml.getConfigurationSection(key);
                                if (sec != null) {
                                    for (String child : sec.getKeys(false)) {
                                        org.bukkit.inventory.ItemStack it = yml.getItemStack(key + "." + child);
                                        if (it != null) lst.add(it.clone());
                                    }
                                }
                            }
                        }
                        this.storage.put(id, lst);
                    } catch (Exception ignore) {}
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to load storage.yml: " + ex.getMessage());
        }
    }

public void saveAll() {
        saveStorage();
        for (PlayerShop ps : shops.values()) {
            save(ps);
        }
    }

    
    private void saveStorage() {
        try {
            org.bukkit.configuration.file.YamlConfiguration yml = new org.bukkit.configuration.file.YamlConfiguration();
            for (java.util.Map.Entry<java.util.UUID, java.util.List<org.bukkit.inventory.ItemStack>> e : storage.entrySet()) {
                java.util.List<String> out = new java.util.ArrayList<>();
                for (org.bukkit.inventory.ItemStack it : e.getValue()) {
                    out.add(com.minkang.ultimate.usershop.util.ItemSerializer.serializeToBase64(it));
                }
                yml.set(e.getKey().toString(), out);
            }
            yml.save(storageFile);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to save storage.yml: " + ex.getMessage());
        }
    }

private void save(PlayerShop ps) {
        try {
            File f = new File(dataDir, ps.getOwner().toString() + ".yml");
            YamlConfiguration yml = new YamlConfiguration();
            ps.toYaml(yml);
            yml.save(f);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save shop: " + e.getMessage());
        }
    }

    public PlayerShop getOrCreateShop(UUID uuid) {
        if (!shops.containsKey(uuid)) {
            PlayerShop ps = new PlayerShop(uuid);
            ps.setSlots(plugin.getConfig().getInt("settings.base-slots", 9));
            shops.put(uuid, ps);
        }
        return shops.get(uuid);
    }

    public PlayerShop getShop(UUID uuid) { return shops.get(uuid); }
    public Collection<PlayerShop> allShops() { return shops.values(); }

    public int getSlotCount(UUID uuid) {
        PlayerShop ps = getOrCreateShop(uuid);
        return ps.getSlots();
    }

    public int getMaxSlotsFor(UUID uuid) { return getSlotCount(uuid); }

    public void setSlotCount(UUID uuid, int slots) {
        PlayerShop ps = getOrCreateShop(uuid);
        ps.setSlots(slots);
        save(ps);
    }

    

public void registerListing(Player player, PlayerShop shop, ItemStack item, int amount, double price, int slot) {
        // SAFETY: prevent item loss when registering to an occupied slot.
        java.util.Map<Integer, com.minkang.ultimate.usershop.model.Listing> map = shop.getListings();
        com.minkang.ultimate.usershop.model.Listing prev = map.get(slot);
if (prev != null) {
    // Refund entire stock of previous listing into owner's storage
    org.bukkit.inventory.ItemStack template = prev.getItem().clone();
    int remaining = prev.getStock();
    int maxStack = template.getMaxStackSize();
    while (remaining > 0) {
        int chunk = Math.min(remaining, maxStack);
        org.bukkit.inventory.ItemStack part = template.clone();
        part.setAmount(chunk);
        addToStorage(shop.getOwner(), part);
        remaining -= chunk;
    }
}
        ItemStack clone = item.clone();
        clone.setAmount(amount);
        com.minkang.ultimate.usershop.model.Listing listing = new com.minkang.ultimate.usershop.model.Listing(clone, price, amount, System.currentTimeMillis());
        map.put(slot, listing);
        save(shop);
        // Discord webhook on register
        if (plugin.getConfig().getBoolean("discord.on-register", true)) {
            String itemName = com.minkang.ultimate.usershop.util.ItemUtils.getPrettyName(clone);
            String seller = player.getName();
            String template = plugin.getConfig().getString("discord.messages.register", "📦 등록: **{seller}** — {item} x{amount} | 가격: {price}");
            String msg = template
                    .replace("{seller}", seller)
                    .replace("{item}", itemName)
                    .replace("{amount}", String.valueOf(amount))
                    .replace("{price}", String.valueOf(price));
            notifyDiscord(msg);

        // 관심 상품 즐겨찾기 알림
        if (plugin.getConfig().getBoolean("favorites.enabled", true)) {
            String baseName = com.minkang.ultimate.usershop.util.ItemUtils.getPrettyName(clone);
            String normalized = com.minkang.ultimate.usershop.util.ItemUtils.normalize(baseName);
            java.util.Map<java.util.UUID, java.util.List<String>> allFav = getAllFavorites();
            for (java.util.Map.Entry<java.util.UUID, java.util.List<String>> entry : allFav.entrySet()) {
                java.util.UUID uid = entry.getKey();
                org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(uid);
                if (target == null || !target.isOnline()) continue;
                for (String kw : entry.getValue()) {
                    if (kw == null || kw.trim().isEmpty()) continue;
                    String normKw = com.minkang.ultimate.usershop.util.ItemUtils.normalize(kw);
                    if (normalized.contains(normKw) || normKw.contains(normalized)) {
                        String title = Main.getInstance().getConfig().getString("favorites.title", "&e[관심 상품 등록]");
                        String subtitle = Main.getInstance().getConfig().getString("favorites.subtitle", "&f{item} &7새 상품이 상점에 등록되었습니다!");
                        String favMsg = Main.getInstance().getConfig().getString("favorites.message", "&e[유저상점] &f{item} &7이(가) 당신의 &d관심 상품&7 키워드에 해당하는 새 상품입니다!");
                        String coloredItem = baseName;
                        target.sendTitle(Main.color(title), Main.color(subtitle.replace("{item}", coloredItem)), 10, 60, 10);
                        target.sendMessage(Main.color(favMsg.replace("{item}", coloredItem)));
                        break;
                    }
                }
            }
        }
        }
    }



    public void unregisterListing(OfflinePlayer player, int slot, boolean refundItem, boolean refundTicket) {
        PlayerShop shop = getOrCreateShop(player.getUniqueId());
        Listing listing = shop.getListings().remove(slot);
        if (listing != null) {
            save(shop);
            if (player.isOnline()) {
                Player online = (Player) player;
                
if (refundItem) {
    int remaining = listing.getStock();
    if (remaining > 0) {
        int maxStack = listing.getItem().getMaxStackSize();
        int givenTotal = 0;
        while (remaining > 0) {
            int take = Math.min(maxStack, remaining);
            org.bukkit.inventory.ItemStack stack = listing.getItem().clone();
            stack.setAmount(take);
            if (!com.minkang.ultimate.usershop.util.ItemUtils.giveItem(online, stack)) {
                addToStorage(online.getUniqueId(), stack);
                online.sendMessage(Main.getInstance().msg("storage-inventory-full"));
            }
            remaining -= take;
            givenTotal += take;
        }
    }
}
                if (refundTicket) {
                    String b64 = plugin.getConfig().getString("items.register-ticket", "");
                    ItemStack ticket = com.minkang.ultimate.usershop.util.ItemSerializer.deserializeFromBase64(b64);
                    if (ticket != null) {
                            if (!ItemUtils.giveItem(online, ticket)) {
                                addToStorage(online.getUniqueId(), ticket);
                                online.sendMessage(Main.getInstance().msg("storage-inventory-full"));
                            }
                        }
                }
            }
        }
    }

    public void handlePurchase(Player buyer, UUID sellerId, int slot, int amount) {
        // Prevent self-purchase: buyer cannot buy their own listings
        if (buyer != null && buyer.getUniqueId().equals(sellerId)) {
            buyer.sendMessage(Main.getInstance().msg("cannot-buy-own"));
            return;
        }

        PlayerShop shop = getOrCreateShop(sellerId);
        Listing listing = shop.getListings().get(slot);
        if (listing == null) return;
        if (listing.getStock() <= 0) {
            buyer.sendMessage(Main.getInstance().msg("out-of-stock"));
            return;
        }
        int buyAmount = amount;
        if (buyAmount > 64) buyAmount = 64;
        if (buyAmount > listing.getStock()) buyAmount = listing.getStock();
        int maxStack = listing.getItem().getMaxStackSize();
        if (buyAmount > maxStack) buyAmount = maxStack;
        if (buyAmount <= 0) {
            buyer.sendMessage(Main.getInstance().msg("out-of-stock"));
            return;
        }

        double priceEach = listing.getPrice();
        double total = priceEach * buyAmount;

        VaultHook vault = plugin.getVault();
        if (vault == null || !vault.isOk()) {
            buyer.sendMessage(Main.color("&c경제 플러그인이 연결되어 있지 않습니다."));
            return;
        }

        if (!vault.has(buyer, total)) {
            buyer.sendMessage(Main.getInstance().msg("purchase-not-enough").replace("{need}", String.valueOf(total)));
            return;
        }

        if (!vault.withdraw(buyer, total)) {
            buyer.sendMessage(Main.getInstance().msg("purchase-not-enough").replace("{need}", String.valueOf(total)));
            return;
        }

        OfflinePlayer seller = Bukkit.getOfflinePlayer(sellerId);
        vault.deposit(seller, total);

        ItemStack give = listing.getItem().clone();
give.setAmount(buyAmount);
// Deliver to personal storage instead of directly to inventory
addToStorage(buyer.getUniqueId(), give);

        listing.setStock(listing.getStock() - buyAmount);
        if (listing.getStock() <= 0) {
            shop.getListings().remove(slot);
        }
        save(shop);

        String itemName = ItemUtils.getPrettyName(give);
        
        // 일일 판매 집계에 반영
        recordDailySale(itemName, priceEach, buyAmount);
        
        if (plugin.getConfig().getBoolean("discord.on-purchase", true)) {
            String tmpl = plugin.getConfig().getString("discord.messages.purchase", "🛒 구매: **{buyer}** — {item} x{amount} | 지불: {paid} | 판매자: {seller}");
            String sellerName = seller.getName() == null ? seller.getUniqueId().toString() : seller.getName();
            String buyerName = buyer.getName();
            String msg2 = tmpl
                    .replace("{buyer}", buyerName)
                    .replace("{seller}", sellerName)
                    .replace("{item}", itemName)
                    .replace("{amount}", String.valueOf(buyAmount))
                    .replace("{paid}", String.valueOf(total));
            notifyDiscord(msg2);
        }
buyer.sendMessage(Main.getInstance().msg("purchase-success")
                .replace("{item}", itemName)
                .replace("{amount}", String.valueOf(buyAmount))
                .replace("{paid}", String.valueOf(total)));
        if (seller.isOnline()) {
            Player sp = (Player) seller;
            sp.sendMessage(Main.getInstance().msg("seller-notify")
                    .replace("{buyer}", buyer.getName())
                    .replace("{item}", itemName)
                    .replace("{amount}", String.valueOf(buyAmount))
                    .replace("{paid}", String.valueOf(total))
                    .replace("{stock}", String.valueOf(listing.getStock())));
        }
    }

    public void setWaitingSearch(UUID id, boolean waiting) {
        if (waiting) searchWaiting.add(id);
        else searchWaiting.remove(id);
    }

    public boolean isWaitingSearch(UUID id) {
        return searchWaiting.contains(id);
    }
    
    public int getCapacity(java.util.UUID uuid) {
        PlayerShop ps = getOrCreateShop(uuid);
        return ps.getSlots();
    }

    public void setCapacity(java.util.UUID uuid, int cap) {
        PlayerShop ps = getOrCreateShop(uuid);
        ps.setSlots(cap);
        save(ps);
    }

    public java.util.List<org.bukkit.inventory.ItemStack> getStorage(java.util.UUID uuid) {
        return storage.computeIfAbsent(uuid, k -> new java.util.ArrayList<>());
    }

    public void addToStorage(java.util.UUID uuid, org.bukkit.inventory.ItemStack item) {
        getStorage(uuid).add(item.clone());
        saveStorage();
    }

    public void removeFromStorage(java.util.UUID uuid, org.bukkit.inventory.ItemStack item) {
        java.util.List<org.bukkit.inventory.ItemStack> lst = getStorage(uuid);
        // Prefer exact match first
        for (int i = 0; i < lst.size(); i++) {
            org.bukkit.inventory.ItemStack it = lst.get(i);
            if (it.isSimilar(item) && it.getAmount() == item.getAmount()) {
                lst.remove(i);
                saveStorage();
                return;
            }
        }
        // Fallback: consume from a larger similar stack
        for (int i = 0; i < lst.size(); i++) {
            org.bukkit.inventory.ItemStack it = lst.get(i);
            if (it.isSimilar(item) && it.getAmount() >= item.getAmount()) {
                int remain = it.getAmount() - item.getAmount();
                if (remain <= 0) lst.remove(i);
                else it.setAmount(remain);
                saveStorage();
                return;
            }
        }
    }
    
    public void sweepExpired() {
        int days = Main.getInstance().getConfig().getInt("expiry.days", 5);
        long ttl = days * 24L * 60L * 60L * 1000L;
        long now = System.currentTimeMillis();

        org.bukkit.configuration.file.FileConfiguration cfg = Main.getInstance().getConfig();
        boolean autoEnabled = cfg.getBoolean("auto-relist.enabled", false);
        int maxCycles = cfg.getInt("auto-relist.max-cycles", 0);
        double multiplier = cfg.getDouble("auto-relist.price-multiplier", 1.0D);
        boolean notify = cfg.getBoolean("auto-relist.notify", true);

        java.util.List<PlayerShop> all = new java.util.ArrayList<>(shops.values());
        for (PlayerShop ps : all) {
            java.util.Map<Integer, com.minkang.ultimate.usershop.model.Listing> map = ps.getListings();
            java.util.List<Integer> toRemove = new java.util.ArrayList<>();
            boolean changed = false;
            for (java.util.Map.Entry<Integer, com.minkang.ultimate.usershop.model.Listing> e : map.entrySet()) {
                com.minkang.ultimate.usershop.model.Listing listing = e.getValue();
                if (now - listing.getCreatedAt() >= ttl) {
                    if (autoEnabled && listing.getStock() > 0 &&
                            (maxCycles <= 0 || listing.getRelistCount() < maxCycles)) {
                        // 자동 재등록: 시간 리셋 + 가격 조정
                        listing.setCreatedAt(now);
                        if (multiplier != 1.0D) {
                            double newPrice = listing.getPrice() * multiplier;
                            if (newPrice < 0.01D) newPrice = 0.01D;
                            listing.setPrice(newPrice);
                        }
                        listing.setRelistCount(listing.getRelistCount() + 1);
                        changed = true;

                        if (notify) {
                            org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(ps.getOwner());
                            if (op.isOnline()) {
                                org.bukkit.entity.Player pl = (org.bukkit.entity.Player) op;
                                pl.sendMessage(Main.getInstance().msg("auto-relisted")
                                        .replace("{item}", com.minkang.ultimate.usershop.util.ItemUtils.getPrettyName(listing.getItem()))
                                        .replace("{price}", String.valueOf(listing.getPrice()))
                                        .replace("{count}", String.valueOf(listing.getRelistCount())));
                            }
                        }
                    } else {
                        // 기존 동작: 보관함으로 이동 후 제거
                        addToStorage(ps.getOwner(), listing.getItem());
                        toRemove.add(e.getKey());
                    }
                }
            }
            for (Integer key : toRemove) {
                map.remove(key);
            }
            if (changed || !toRemove.isEmpty()) {
                save(ps);
            }
        }
    }
    

    // === 전국 평균 시세 계산용 DTO ===
    public static class PriceStats {
        public final double min;
        public final double max;
        public final double avg;
        public final int count;

        public PriceStats(double min, double max, double avg, int count) {
            this.min = min;
            this.max = max;
            this.avg = avg;
            this.count = count;
        }
    }

    /**
     * 동일 아이템(유사 아이템) 기준으로 전국 평균 시세/최저가/등록 개수 계산.
     * - ItemStack#isSimilar 를 사용하여 타입/메타가 같은지 비교합니다.
     */
    public PriceStats computePriceStats(org.bukkit.inventory.ItemStack target) {
        if (target == null) return new PriceStats(0, 0, 0, 0);
        double min = Double.MAX_VALUE;
        double max = 0.0D;
        double sum = 0.0D;
        int count = 0;
        for (PlayerShop ps : shops.values()) {
            for (java.util.Map.Entry<Integer, Listing> e : ps.getListings().entrySet()) {
                Listing l = e.getValue();
                if (l == null || l.getItem() == null) continue;
                org.bukkit.inventory.ItemStack base = l.getItem().clone();
                org.bukkit.inventory.ItemStack cmp = target.clone();
                base.setAmount(1);
                cmp.setAmount(1);
                if (base.isSimilar(cmp)) {
                    double price = l.getPrice();
                    if (price < 0) continue;
                    if (price < min) min = price;
                    if (price > max) max = price;
                    sum += price;
                    count++;
                }
            }
        }
        if (count == 0) {
            return new PriceStats(0, 0, 0, 0);
        }
        double avg = sum / count;
        return new PriceStats(min, max, avg, count);
    }

    // === 일일 판매 집계 업데이트 ===
    private void recordDailySale(String displayName, double priceEach, int amount) {
        try {
            if (displayName == null || displayName.isEmpty()) {
                displayName = "알 수 없는 아이템";
            }
            LocalDate now = LocalDate.now(salesZoneId);
            // 날짜가 바뀌었으면 이전 날짜 요약을 한 번 보내고 초기화
            if (!now.equals(salesDate)) {
                if (lastSummaryDate == null || !lastSummaryDate.equals(salesDate)) {
                    sendDailySalesSummaryInternal(salesDate);
                }
                dailySales.clear();
                salesDate = now;
            }
            String key = ItemUtils.normalize(displayName);
            DailySaleStats stats = dailySales.get(key);
            if (stats == null) {
                stats = new DailySaleStats();
                stats.displayName = displayName;
                stats.minPrice = priceEach;
                stats.maxPrice = priceEach;
                stats.totalPrice = priceEach;
                stats.totalTrades = 1;
                stats.totalAmount = amount;
                dailySales.put(key, stats);
            } else {
                if (priceEach < stats.minPrice) stats.minPrice = priceEach;
                if (priceEach > stats.maxPrice) stats.maxPrice = priceEach;
                stats.totalPrice += priceEach;
                stats.totalTrades += 1;
                stats.totalAmount += amount;
            }
        } catch (Exception ignored) {
        }
    }

    // === 일일 판매 요약 디스코드 전송 ===
    private void sendDailySalesSummaryInternal(LocalDate date) {
        try {
            if (dailySales.isEmpty()) {
                lastSummaryDate = date;
                return;
            }
            if (!plugin.getConfig().getBoolean("discord.enabled", true)) {
                lastSummaryDate = date;
                return;
            }
            if (!plugin.getConfig().getBoolean("discord.daily-summary-enabled", true)) {
                lastSummaryDate = date;
                return;
            }
            String headerTmpl = plugin.getConfig().getString(
                    "discord.daily-summary-header",
                    "📊 유저상점 일일 판매 요약 ({date})"
            );
            String lineTmpl = plugin.getConfig().getString(
                    "discord.daily-summary-line",
                    "- {item} | 평균 {avg}원 (최저 {min}, 최고 {max}, 거래 {trades}회, 판매수량 {amount}개)"
            );
            StringBuilder sb = new StringBuilder();
            sb.append(headerTmpl.replace("{date}", date.toString()));

            java.util.List<java.util.Map.Entry<String, DailySaleStats>> entries =
                    new java.util.ArrayList<>(dailySales.entrySet());
            // 판매 수량 기준 내림차순 정렬
            entries.sort((a, b) -> Integer.compare(
                    b.getValue().totalAmount,
                    a.getValue().totalAmount
            ));
            int index = 0;
            for (java.util.Map.Entry<String, DailySaleStats> e : entries) {
                DailySaleStats s = e.getValue();
                if (s.totalTrades <= 0) continue;
                double avg = s.totalPrice / s.totalTrades;
                String line = lineTmpl
                        .replace("{item}", s.displayName != null ? s.displayName : e.getKey())
                        .replace("{avg}", String.format(java.util.Locale.KOREA, "%.1f", avg))
                        .replace("{min}", String.format(java.util.Locale.KOREA, "%.1f", s.minPrice))
                        .replace("{max}", String.format(java.util.Locale.KOREA, "%.1f", s.maxPrice))
                        .replace("{trades}", String.valueOf(s.totalTrades))
                        .replace("{amount}", String.valueOf(s.totalAmount));
                if (sb.length() + line.length() + 1 > 1800) {
                    sb.append("\n... 등 ").append(entries.size() - index).append("개 더");
                    break;
                }
                sb.append("\n").append(line);
                index++;
            }
            notifyDiscord(sb.toString());
            lastSummaryDate = date;
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to build daily sales summary: " + ex.getMessage());
            lastSummaryDate = date;
        }
    }


    // === 디스코드 웹훅 헬퍼 ===
    private void notifyDiscord(String content) {
        try {
            String url = plugin.getConfig().getString("discord.webhook-url", "");
            if (url == null || url.isEmpty()) return;
            DiscordWebhook.send(url, content);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to send discord webhook: " + ex.getMessage());
        }
    }

    // === 관심 상품 즐겨찾기 로직 ===
    private void ensureFavoritesLoaded() {
        if (favoritesLoaded) return;
        favoritesLoaded = true;
        favorites.clear();
        if (favoritesFile == null || !favoritesFile.exists()) return;
        try {
            org.bukkit.configuration.file.YamlConfiguration yml =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(favoritesFile);
            if (yml.isConfigurationSection("favorites")) {
                for (String key : yml.getConfigurationSection("favorites").getKeys(false)) {
                    try {
                        java.util.UUID id = java.util.UUID.fromString(key);
                        java.util.List<String> list = yml.getStringList("favorites." + key);
                        favorites.put(id, new java.util.ArrayList<>(list));
                    } catch (IllegalArgumentException ignore) {}
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to load favorites.yml: " + ex.getMessage());
        }
    }

    private void saveFavorites() {
        if (favoritesFile == null) return;
        try {
            org.bukkit.configuration.file.YamlConfiguration yml = new org.bukkit.configuration.file.YamlConfiguration();
            for (java.util.Map.Entry<java.util.UUID, java.util.List<String>> e : favorites.entrySet()) {
                yml.set("favorites." + e.getKey().toString(), new java.util.ArrayList<>(e.getValue()));
            }
            yml.save(favoritesFile);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to save favorites.yml: " + ex.getMessage());
        }
    }

    public java.util.List<String> getFavoriteKeywords(java.util.UUID uuid) {
        ensureFavoritesLoaded();
        return favorites.computeIfAbsent(uuid, k -> new java.util.ArrayList<>());
    }

    public java.util.Map<java.util.UUID, java.util.List<String>> getAllFavorites() {
        ensureFavoritesLoaded();
        return favorites;
    }

    public void addFavoriteKeyword(java.util.UUID uuid, String keyword) {
        if (keyword == null) return;
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) return;
        int max = plugin.getConfig().getInt("favorites.max-per-player", 10);
        java.util.List<String> list = getFavoriteKeywords(uuid);
        if (list.contains(trimmed)) return;
        if (max > 0 && list.size() >= max) {
            return;
        }
        list.add(trimmed);
        saveFavorites();
    }

    public boolean removeFavoriteKeyword(java.util.UUID uuid, String keyword) {
        if (keyword == null) return false;
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) return false;
        java.util.List<String> list = getFavoriteKeywords(uuid);
        boolean removed = list.remove(trimmed);
        if (removed) {
            saveFavorites();
        }
        return removed;
    }


}