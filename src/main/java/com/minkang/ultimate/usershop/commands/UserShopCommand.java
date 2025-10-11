package com.minkang.ultimate.usershop.commands;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.data.ShopManager;
import com.minkang.ultimate.usershop.gui.MainShopsGUI;
import com.minkang.ultimate.usershop.model.PlayerShop;
import com.minkang.ultimate.usershop.util.ItemUtils;
import com.minkang.ultimate.usershop.util.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class UserShopCommand implements CommandExecutor {

    private org.bukkit.inventory.ItemStack buildTicket(int amount) {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        String mat = cfg.getString("expansion-ticket.material", "PAPER");
        String name = cfg.getString("expansion-ticket.name", "&a유저상점 확장권");
        java.util.List<String> lore = cfg.getStringList("expansion-ticket.lore");
        org.bukkit.inventory.ItemStack it = new org.bukkit.inventory.ItemStack(org.bukkit.Material.matchMaterial(mat) == null ? org.bukkit.Material.PAPER : org.bukkit.Material.matchMaterial(mat), Math.max(1, amount));
        org.bukkit.inventory.meta.ItemMeta im = it.getItemMeta();
        if (im != null) {
            im.setDisplayName(com.minkang.ultimate.usershop.Main.color(name));
            java.util.List<String> out = new java.util.ArrayList<>();
            for (String l : lore) out.add(com.minkang.ultimate.usershop.Main.color(l));
            im.setLore(out);
            it.setItemMeta(im);
        }
        return it;
    }
    private boolean isTicket(org.bukkit.inventory.ItemStack it) {
        if (it == null || it.getType() == org.bukkit.Material.AIR) return false;
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        String name = com.minkang.ultimate.usershop.Main.color(cfg.getString("expansion-ticket.name", "&a유저상점 확장권"));
        java.util.List<String> lore = cfg.getStringList("expansion-ticket.lore");
        org.bukkit.inventory.meta.ItemMeta im = it.getItemMeta();
        if (im == null || !im.hasDisplayName()) return false;
        if (!name.equals(im.getDisplayName())) return false;
        if (!lore.isEmpty()) {
            java.util.List<String> got = im.getLore();
            if (got == null) return false;
            java.util.List<String> colored = new java.util.ArrayList<>();
            for (String l : lore) colored.add(com.minkang.ultimate.usershop.Main.color(l));
            if (!got.containsAll(colored)) return false;
        }
        return true;
    }


    private final Main plugin;

    public UserShopCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        ShopManager sm = plugin.getShopManager();

        if (!(sender instanceof Player)) {
            sender.sendMessage(Main.color(plugin.getConfig().getString("messages.prefix","") + plugin.getConfig().getString("messages.not-player")));
            return true;
        }
        Player p = (Player) sender;

        if (args.length == 0 || "도움말".equals(args[0])) {
            for (String line : plugin.getConfig().getStringList("messages.help")) {
                p.sendMessage(Main.color(line));
            }
            return true;
        }

        if ("보관함".equals(args[0])) {
            new com.minkang.ultimate.usershop.gui.StorageGUI(plugin, p).open();
            return true;
        }
        if ("열기".equals(args[0])) {
            new MainShopsGUI(plugin, p).open(0);
            p.sendMessage(plugin.msg("open"));
            return true;
        }

        if ("등록".equals(args[0])) {
            if (args.length < 4) {
                p.sendMessage(Main.color("&c사용법: /유저상점 등록 <가격> <갯수> <슬롯>"));
                return true;
            }
            double price;
            int amount;
            int slot;
            try {
                price = Double.parseDouble(args[1]);
                amount = Integer.parseInt(args[2]);
                slot = Integer.parseInt(args[3]);
            } catch (Exception ex) {
                p.sendMessage(plugin.msg("invalid-number"));
                return true;
            }
                        
            ItemStack inHand = p.getInventory().getItemInMainHand();
            if (inHand == null || inHand.getType().name().equals("AIR")) {
                p.sendMessage(plugin.msg("no-item-in-hand"));return true;
            }
            if (inHand.getAmount() < amount) {
                p.sendMessage(plugin.msg("not-enough-item"));return true;
            }
            PlayerShop shop = sm.getOrCreateShop(p.getUniqueId());
            int cap = sm.getCapacity(p.getUniqueId());
            if (shop.getListings().size() >= cap) {
                p.sendMessage(Main.color("&c등록 가능 개수를 초과했습니다. 현재: " + cap));
                return true;
            }
            double regFee = plugin.getConfig().getDouble("fees.register", 500.0);
            if (regFee > 0) {
                if (plugin.getVault()==null || !plugin.getVault().isOk() || !plugin.getVault().has(p, regFee) || !plugin.getVault().withdraw(p, regFee)) {
                    p.sendMessage(Main.color("&c등록 수수료가 부족합니다. 필요: " + regFee));
                    return true;
                }
                p.sendMessage(Main.color(plugin.getConfig().getString("messages.prefix","") + plugin.getConfig().getString("messages.register-fee-charged", "&7등록 수수료 &e{fee}&7원이 차감되었습니다.").replace("{fee}", String.valueOf(regFee))));
            }
            int maxSlot = 54; if (slot < 0 || slot >= maxSlot) {
                p.sendMessage(plugin.msg("invalid-slot"));return true;
            }
            sm.registerListing(p, shop, inHand, amount, price, slot);
            // remove items in hand
            ItemStack copy = inHand.clone();
            int newAmt = inHand.getAmount() - amount;
            if (newAmt <= 0) {
                p.getInventory().setItemInMainHand(null);
            } else {
                copy.setAmount(newAmt);
                p.getInventory().setItemInMainHand(copy);
            }
            p.sendMessage(Main.color(plugin.getConfig().getString("messages.prefix","") +
                    plugin.getConfig().getString("messages.registered")
                            .replace("{slot}", String.valueOf(slot))
                            .replace("{price}", String.valueOf(price))
                            .replace("{amount}", String.valueOf(amount))));
            // broadcast
            String itemName = ItemUtils.getPrettyName(inHand);
            Bukkit.broadcastMessage(Main.color(plugin.getConfig().getString("messages.prefix","") +
                    plugin.getConfig().getString("messages.broadcast-registered")
                            .replace("{player}", p.getName())
                            .replace("{item}", itemName)
                            .replace("{amount}", String.valueOf(amount))
                            .replace("{price}", String.valueOf(price))));
            return true;
        }

        if ("등록취소".equals(args[0])) {
            if (args.length < 2) {
                p.sendMessage(Main.color("&c사용법: /유저상점 등록취소 <슬롯>"));
                return true;
            }
            int slot;
            try {
                slot = Integer.parseInt(args[1]);
            } catch (Exception ex) {
                p.sendMessage(plugin.msg("invalid-number"));
                return true;
            }
            plugin.getShopManager().unregisterListing(p, slot, true, true);
            p.sendMessage(Main.color(plugin.getConfig().getString("messages.prefix","") +
                    plugin.getConfig().getString("messages.unregistered").replace("{slot}", String.valueOf(slot))));
            return true;
        }

        if ("확장".equals(args[0])) {
            ItemStack ticket = ItemSerializer.deserializeFromBase64(plugin.getConfig().getString("items.expand-ticket", ""));
            if (ticket == null || !ItemUtils.consumeOne(p, ticket)) {
                p.sendMessage(plugin.msg("need-expand-ticket"));
                return true;
            }
            int current = sm.getSlotCount(p.getUniqueId());
            int max = plugin.getConfig().getInt("settings.max-slots", 54);
            int step = 9;
            int next = current + step;
            if (next > max) {
                p.sendMessage(plugin.msg("max-expanded").replace("{max}", String.valueOf(max)));
                if (!ItemUtils.giveItem(p, ticket)) {
                plugin.getShopManager().addToStorage(p.getUniqueId(), ticket);
                p.sendMessage(plugin.msg("storage-inventory-full"));
                }
                return true;
            }
            sm.setSlotCount(p.getUniqueId(), next);
            p.sendMessage(plugin.msg("expanded").replace("{slots}", String.valueOf(next)));
            return true;
        }

        if ("설정".equals(args[0])) {
            if (!p.isOp() && !p.hasPermission("usershop.admin")) {
                p.sendMessage(Main.color("&c권한이 없습니다."));
                return true;
            }
            if (args.length < 2) {
                p.sendMessage(Main.color("&c사용법: /유저상점 설정 <오픈|등록권|확장권|확장삭제|등록취소> ..."));
                return true;
            }
            String sub = args[1];
            if ("오픈".equals(sub)) {
                ItemStack inHand = p.getInventory().getItemInMainHand();
                if (inHand == null || inHand.getType().name().equals("AIR")) {
                    p.sendMessage(plugin.msg("no-item-in-hand"));
                    return true;
                }
                String b64 = ItemSerializer.serializeToBase64(inHand);
                plugin.getConfig().set("items.open-item", b64);
                plugin.saveConfig();
                p.sendMessage(plugin.msg("set-open-item"));
                return true;
            }
            if ("등록권".equals(sub)) {
                ItemStack inHand = p.getInventory().getItemInMainHand();
                if (inHand == null || inHand.getType().name().equals("AIR")) {
                    p.sendMessage(plugin.msg("no-item-in-hand"));
                    return true;
                }
                String b64 = ItemSerializer.serializeToBase64(inHand);
                plugin.getConfig().set("items.register-ticket", b64);
                plugin.saveConfig();
                p.sendMessage(plugin.msg("set-register-ticket"));
                return true;
            }
            if ("확장권".equals(sub)) {
                ItemStack inHand = p.getInventory().getItemInMainHand();
                if (inHand == null || inHand.getType().name().equals("AIR")) {
                    p.sendMessage(plugin.msg("no-item-in-hand"));
                    return true;
                }
                String b64 = ItemSerializer.serializeToBase64(inHand);
                plugin.getConfig().set("items.expand-ticket", b64);
                plugin.saveConfig();
                p.sendMessage(plugin.msg("set-expand-ticket"));
                return true;
            }
            if ("확장삭제".equals(sub)) {
                if (args.length < 4) {
                    p.sendMessage(Main.color("&c사용법: /유저상점 설정 확장삭제 <플레이어> <횟수>"));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                int count;
                try {
                    count = Integer.parseInt(args[3]);
                } catch (Exception ex) {
                    p.sendMessage(plugin.msg("invalid-number"));
                    return true;
                }
                int current = plugin.getShopManager().getSlotCount(target.getUniqueId());
                int base = plugin.getConfig().getInt("settings.base-slots", 9);
                int newSlots = current - (count * 9);
                if (newSlots < base) newSlots = base;
                plugin.getShopManager().setSlotCount(target.getUniqueId(), newSlots);
                p.sendMessage(plugin.msg("reduced-expansion")
                        .replace("{player}", target.getName() == null ? target.getUniqueId().toString() : target.getName())
                        .replace("{count}", String.valueOf(count))
                        .replace("{slots}", String.valueOf(newSlots)));
                return true;
            }
            if ("등록취소".equals(sub)) {
                if (args.length < 4) {
                    p.sendMessage(Main.color("&c사용법: /유저상점 설정 등록취소 <플레이어> <슬롯>"));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                int slot;
                try {
                    slot = Integer.parseInt(args[3]);
                } catch (Exception ex) {
                    p.sendMessage(plugin.msg("invalid-number"));
                    return true;
                }
                plugin.getShopManager().unregisterListing(target, slot, true, true);
                p.sendMessage(plugin.msg("admin-removed").replace("{slot}", String.valueOf(slot)));
                return true;
            }
            return true;
        }


        // admin reload
        if (args.length >= 1 && (args[0].equalsIgnoreCase("리로드") || args[0].equalsIgnoreCase("reload"))) {
            if (!p.hasPermission("usershop.admin")) {
                p.sendMessage(Main.color("&c권한이 없습니다.")); return true;
            }
            plugin.reloadConfig();
            p.sendMessage(plugin.msg("reloaded"));
            return true;
        }

        // admin give ticket: /유저상점 확장권 <수량>
        if (args.length >= 1 && (args[0].equalsIgnoreCase("확장권"))) {
            if (!p.hasPermission("usershop.admin")) {
                p.sendMessage(Main.color("&c권한이 없습니다.")); return true;
            }
            int count = 1;
            if (args.length >= 2) {
                try { count = Integer.parseInt(args[1]); } catch (Exception ignored) {}
            }
            org.bukkit.inventory.ItemStack ticket = buildTicket(count);
            org.bukkit.inventory.PlayerInventory inv = p.getInventory();
            java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> left = inv.addItem(ticket);
            if (!left.isEmpty()) {
                // drop leftovers
                for (org.bukkit.inventory.ItemStack rest : left.values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), rest);
                }
            }
            p.sendMessage(plugin.msg("give-ticket").replace("{count}", String.valueOf(count)));
            return true;
        }

        // user consume ticket: /유저상점 확장 [수량]
        if (args.length >= 1 && (args[0].equalsIgnoreCase("확장"))) {
            int want = -1;
            if (args.length >= 2) {
                try { want = Integer.parseInt(args[1]); } catch (Exception ignored) {}
            }
            org.bukkit.inventory.ItemStack hand = p.getInventory().getItemInMainHand();
            if (!isTicket(hand)) {
                p.sendMessage(plugin.msg("not-ticket")); return true;
            }
            int have = hand.getAmount();
            int use = (want <= 0 ? have : Math.min(want, have));
            int per = plugin.getConfig().getInt("expansion-ticket.slots-per-ticket", 9);
            int current = plugin.getShopManager().getCapacity(p.getUniqueId());
            int max = plugin.getConfig().getInt("settings.max-slots", 54);
            long added = (long) use * per;
            int newSlots = current + (int) added;
            if (newSlots > max) {
                p.sendMessage(plugin.msg("expansion-over-max").replace("{max}", String.valueOf(max))); return true;
            }
            // consume
            hand.setAmount(have - use);
            plugin.getShopManager().setCapacity(p.getUniqueId(), newSlots);
            p.sendMessage(plugin.msg("expansion-success")
                .replace("{added}", String.valueOf(added))
                .replace("{new}", String.valueOf(newSlots)));
            return true;
        }

        // fallback
        for (String line : plugin.getConfig().getStringList("messages.help")) {
            p.sendMessage(Main.color(line));
        }
        return true;
    }
}