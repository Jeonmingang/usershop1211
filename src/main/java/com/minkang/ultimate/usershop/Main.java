package com.minkang.ultimate.usershop;

import com.minkang.ultimate.usershop.commands.UserShopCommand;
import com.minkang.ultimate.usershop.data.ShopManager;
import com.minkang.ultimate.usershop.listeners.ChatListener;
import com.minkang.ultimate.usershop.listeners.GuiListener;
import com.minkang.ultimate.usershop.listeners.InteractListener;
import com.minkang.ultimate.usershop.util.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;
    private ShopManager shopManager;
    private VaultHook vault;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        // legacy translations file
        saveResource("translations.yml", false);

        this.vault = new VaultHook(this);
        this.vault.setup();

        this.shopManager = new ShopManager(this);
        this.shopManager.loadAll();
        // periodic expiry sweep
        org.bukkit.Bukkit.getScheduler().runTaskTimer(this, () -> shopManager.sweepExpired(), 20L*60, 20L*600);

        getCommand("유저상점").setExecutor(new UserShopCommand(this));
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new InteractListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);

        getLogger().info("UltimateUserShop v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (shopManager != null) {
            shopManager.saveAll();
        }
        getLogger().info("UltimateUserShop disabled.");
    }

    public static Main getInstance() {
        return instance;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public VaultHook getVault() {
        return vault;
    }

    public String msg(String key) {
        FileConfiguration cfg = getConfig();
        String prefix = cfg.getString("messages.prefix", "");
        String path = "messages." + key;
        String v = cfg.getString(path, path);
        if (v == null) v = path;
        return color(prefix + v);
    }

    public static String color(String s) {
        return s.replace("&", "§");
    }
}
