package com.minkang.ultimate.usershop.util;

import com.minkang.ultimate.usershop.Main;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {

    private final Main plugin;
    private Economy economy;
    private boolean ok = false;

    public VaultHook(Main plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        if (plugin.getConfig().getBoolean("economy.enabled", true)) {
            if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
                plugin.getLogger().warning("Vault not found. Economy disabled.");
                return;
            }
            RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                plugin.getLogger().warning("No Economy provider found. Economy disabled.");
                return;
            }
            economy = rsp.getProvider();
            ok = economy != null;
        }
    }

    public boolean isOk() { return ok && economy != null; }

    public boolean has(Player p, double amount) {
        if (!isOk()) return false;
        try { return economy.has(p, amount); } catch (Throwable t) { return economy.getBalance(p) >= amount; }
    }

    public boolean has(OfflinePlayer p, double amount) {
        if (!isOk()) return false;
        try { return economy.has(p, amount); } catch (Throwable t) { return economy.getBalance(p) >= amount; }
    }

    public boolean withdraw(Player p, double amount) {
        if (!isOk()) return false;
        return economy.withdrawPlayer(p, amount).transactionSuccess();
    }

    public boolean withdraw(OfflinePlayer p, double amount) {
        if (!isOk()) return false;
        return economy.withdrawPlayer(p, amount).transactionSuccess();
    }

    public boolean deposit(OfflinePlayer p, double amount) {
        if (!isOk()) return false;
        return economy.depositPlayer(p, amount).transactionSuccess();
    }

    public boolean deposit(Player p, double amount) {
        if (!isOk()) return false;
        return economy.depositPlayer(p, amount).transactionSuccess();
    }
}
