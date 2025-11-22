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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ShopManager {
    private final java.util.Map<java.util.UUID, java.util.List<org.bukkit.inventory.ItemStack>> storage = new java.util.concurrent.ConcurrentHashMap<>();

    private final Main plugin;
    private final File dataDir;
    
    private final java.io.File storageFile;
private final Map<UUID, PlayerShop> shops = new ConcurrentHashMap<>();
    private final Set<UUID> searchWaiting = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());

    public ShopManager(Main plugin) {
        this.plugin = plugin;
        this.dataDir = new File(plugin.getDataFolder(), "shops");
        if (!dataDir.exists()) dataDir.mkdirs();
            this.storageFile = new java.io.File(plugin.getDataFolder(), "storage.yml");
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
                    .replace("{paid}", String.valueOf(total)));
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
        java.util.List<PlayerShop> all = new java.util.ArrayList<>(shops.values());
        for (PlayerShop ps : all) {
            java.util.Map<Integer, com.minkang.ultimate.usershop.model.Listing> map = ps.getListings();
            java.util.List<Integer> toRemove = new java.util.ArrayList<>();
            for (java.util.Map.Entry<Integer, com.minkang.ultimate.usershop.model.Listing> e : map.entrySet()) {
                if (now - e.getValue().getCreatedAt() >= ttl) {
                    addToStorage(ps.getOwner(), e.getValue().getItem());
                    toRemove.add(e.getKey());
                }
            }
            for (Integer key : toRemove) map.remove(key);
            if (!toRemove.isEmpty()) save(ps);
        }
    }


    private void notifyDiscord(String text) {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        boolean enabled = cfg.getBoolean("discord.enabled", false);
        if (!enabled) return;
        String url = cfg.getString("discord.webhook-url", "");
        if (url == null || url.isEmpty()) return;
        com.minkang.ultimate.usershop.util.DiscordWebhook.send(url, text);
    }
    
}
