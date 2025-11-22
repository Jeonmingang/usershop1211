
package com.minkang.ultimate.menu;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private static Main instance;
    private MenuManager menuManager;

    public static Main get(){ return instance; }
    public MenuManager menu(){ return menuManager; }

    @Override
    public void onEnable(){
        instance = this;
        saveDefaultConfig();

        // Manager
        menuManager = new MenuManager(this);
        menuManager.reload();

        // Listeners
        Bukkit.getPluginManager().registerEvents(new MenuClickListener(menuManager), this);
        Bukkit.getPluginManager().registerEvents(new BoundItemListener(menuManager), this);
        if (menuManager.isOpenOnShiftF()) {
            Bukkit.getPluginManager().registerEvents(new ShiftFListener(menuManager), this);
        }

        // Commands
        if (getCommand("메뉴") != null){
            getCommand("메뉴").setExecutor(new MenuCommand(this, menuManager));
        }
        if (getCommand("야생랜덤") != null){
            getCommand("야생랜덤").setExecutor(new RandomWildCommand(this));
        }

        getLogger().info("[Menu1211] Enabled");
    }

    @Override
    public void onDisable(){
        getLogger().info("[Menu1211] Disabled");
    }
}
