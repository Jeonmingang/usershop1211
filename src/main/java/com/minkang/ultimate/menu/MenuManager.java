package com.minkang.ultimate.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.logging.Logger;

public class MenuManager {
    private final Main plugin;
    private String titleColored;
    private int size;
    private boolean openOnShiftF;
    private RunAs defaultRunAs = RunAs.CONSOLE;

    private final Map<Integer, MenuButton> buttons = new HashMap<>();

    enum RunAs { PLAYER, CONSOLE, OP_PLAYER }

    public MenuManager(Main plugin){
        this.plugin = plugin;
    }

    public boolean isOpenOnShiftF(){ return openOnShiftF; }
    public String getMenuTitleColored(){ return titleColored; }

    public void reload(){
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        String rawTitle = c.getString("menu.title", "&8&lMenu");
        titleColored = ChatColor.translateAlternateColorCodes('&', rawTitle);
        size = Math.max(9, Math.min(54, c.getInt("menu.size", 27)));
        openOnShiftF = c.getBoolean("menu.open_on_shift_f", true);

        String runAs = c.getString("menu.default-run-as", "CONSOLE");
        try {
            defaultRunAs = RunAs.valueOf(runAs.toUpperCase(Locale.ROOT));
        } catch (Exception e){
            defaultRunAs = RunAs.CONSOLE;
        }

        buttons.clear();
        ConfigurationSection items = c.getConfigurationSection("menu.items");
        Logger log = plugin.getLogger();
        if (items == null || items.getKeys(false).isEmpty()){
            log.warning("[Menu1211] menu.items 가 비어있습니다. GUI가 빈칸으로 보일 수 있습니다.");
            return;
        }

        for (String key : items.getKeys(false)){
            ConfigurationSection s = items.getConfigurationSection(key);
            if (s == null) continue;

            int slot = s.getInt("slot", -1);
            if (slot < 0 || slot >= size){
                log.warning("[Menu1211] " + key + " 의 slot이 범위를 벗어났습니다: " + slot);
                continue;
            }

            String matName = s.getString("material", "BARRIER");
            Material mat = Material.matchMaterial(matName);
            if (mat == null || mat.isAir()){
                log.warning("[Menu1211] " + key + " 의 material 이 잘못되었습니다: " + matName);
                mat = Material.BARRIER;
            }

            String nameRaw = s.getString("name", "&fButton");
            List<String> loreRaw = s.getStringList("lore");
            String cmd = s.getString("command", "");
            boolean close = s.getBoolean("close", true);
            String runAsKey = s.getString("run-as", null);

            RunAs run = runAsKey != null ? parseRunAs(runAsKey, defaultRunAs) : defaultRunAs;

            buttons.put(slot, new MenuButton(mat, nameRaw, loreRaw, cmd, close, run));
        }
    }

    private RunAs parseRunAs(String s, RunAs fallback){
        try {
            return RunAs.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (Exception e){
            return fallback;
        }
    }

    /** InventoryHolder 로 안전 판별 */
    static class Holder implements InventoryHolder {
        @Override public Inventory getInventory(){ return null; }
    }

    /** 외부에서 메뉴 인벤토리인지 확인할 때 호출 */
    public boolean isMenuInventory(Inventory inv){
        return inv != null && inv.getHolder() instanceof Holder;
    }

    public void open(Player p){
        Inventory inv = Bukkit.createInventory(
                new Holder(),
                size,
                LegacyComponentSerializer.legacyAmpersand().deserialize(titleColored)
        );

        for (Map.Entry<Integer, MenuButton> e : buttons.entrySet()){
            int slot = e.getKey();
            MenuButton b = e.getValue();

            ItemStack it = new ItemStack(b.material);
            ItemMeta meta = it.getItemMeta();
            if (meta != null){
                meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(
                        b.nameRaw.replace("{player}", p.getName())
                ));
                if (b.loreRaw != null && !b.loreRaw.isEmpty()){
                    List<Component> lore = new ArrayList<>();
                    for (String ln : b.loreRaw){
                        lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(
                                ln.replace("{player}", p.getName())
                        ));
                    }
                    meta.lore(lore);
                }
                meta.addItemFlags(ItemFlag.values());
                it.setItemMeta(meta);
            }
            inv.setItem(slot, it);
        }

        p.openInventory(inv);
    }

    
    public void handleClick(Player p, Inventory inv, int rawSlot){
        if (!isMenuInventory(inv)) return;
        MenuButton b = buttons.get(rawSlot);
        if (b == null) return;

        String cmd = b.command.replace("{player}", p.getName()).trim();
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1).trim();
        }

        if (!cmd.isEmpty()) {
            // 유저상점 GUI 명령어는 항상 플레이어 기준으로 실행되도록 강제
            String lower = cmd.toLowerCase(java.util.Locale.ROOT);
            RunAs effectiveRunAs = b.runAs;
            if (lower.startsWith("유저상점") || lower.startsWith("usershop") || lower.startsWith("user-shop")) {
                effectiveRunAs = RunAs.PLAYER;
            }

            switch (effectiveRunAs) {
                case CONSOLE:
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    break;
                case PLAYER:
                    p.performCommand(cmd);
                    break;
                case OP_PLAYER:
                    boolean wasOp = p.isOp();
                    try {
                        if (!wasOp) {
                            p.setOp(true);
                        }
                        Bukkit.dispatchCommand(p, cmd);
                    } finally {
                        if (!wasOp) {
                            p.setOp(false);
                        }
                    }
                    break;
            }
        }

        if (b.closeOnClick) {
            p.closeInventory();
        }
    }

    static class MenuButton {
        final Material material;
        final String nameRaw;
        final List<String> loreRaw;
        final String command;
        final boolean closeOnClick;
        final RunAs runAs;

        MenuButton(Material material, String nameRaw, List<String> loreRaw, String command, boolean closeOnClick, RunAs runAs){
            this.material = material;
            this.nameRaw = nameRaw;
            this.loreRaw = loreRaw != null ? loreRaw : Collections.emptyList();
            this.command = command != null ? command : "";
            this.closeOnClick = closeOnClick;
            this.runAs = runAs;
        }
    }
}