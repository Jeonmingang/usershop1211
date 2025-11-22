
package com.minkang.ultimate.menu;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class MenuCommand implements CommandExecutor {
    private final Main plugin;
    private final MenuManager menu;

    public MenuCommand(Main plugin, MenuManager menu){
        this.plugin = plugin;
        this.menu = menu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if (args.length >= 1 && args[0].equalsIgnoreCase("리로드")){
            if (!sender.hasPermission("menu.reload")){
                sender.sendMessage(ChatColor.RED + "권한이 없습니다.");
                return true;
            }
            plugin.reloadConfig();
            menu.reload();
            sender.sendMessage(ChatColor.GREEN + "메뉴 설정을 리로드했습니다.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("아이템")){
            if (!(sender instanceof Player)){
                sender.sendMessage(ChatColor.RED + "플레이어만 가능합니다.");
                return true;
            }
            if (!sender.hasPermission("menu.assign")){
                sender.sendMessage(ChatColor.RED + "권한이 없습니다.");
                return true;
            }
            Player p = (Player) sender;
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()){
                p.sendMessage(ChatColor.YELLOW + "손에 든 아이템이 없습니다.");
                return true;
            }
            ItemMeta meta = hand.getItemMeta();
            if (meta == null){
                p.sendMessage(ChatColor.YELLOW + "이 아이템은 메타를 지원하지 않습니다.");
                return true;
            }
            meta.getPersistentDataContainer().set(new NamespacedKey(Main.get(), "menu_opener"), PersistentDataType.BYTE, (byte)1);
            hand.setItemMeta(meta);
            p.sendMessage(ChatColor.AQUA + "이 아이템을 우클릭하면 메뉴가 열립니다.");
            return true;
        }

        // 기본: 메뉴 열기
        if (sender instanceof Player p){
            menu.open(p);
        } else {
            sender.sendMessage(ChatColor.YELLOW + "/메뉴 : 플레이어만 사용 가능합니다.");
        }
        return true;
    }
}
