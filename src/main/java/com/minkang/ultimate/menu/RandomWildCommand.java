
package com.minkang.ultimate.menu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import net.kyori.adventure.title.Title;
import java.time.Duration;
import org.bukkit.Sound;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RandomWildCommand implements CommandExecutor {
    private final Main plugin;
    // cooldowns: playerUUID -> next usable epoch millis
    private final Map<UUID, Long> nextUse = new HashMap<>();

    public RandomWildCommand(Main plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if (!(sender instanceof Player p)){
            sender.sendMessage("플레이어만 사용 가능합니다.");
            return true;
        }

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("random_wild");
        if (sec == null){
            p.sendMessage(ChatColor.RED + "랜덤야생 설정이 없습니다.");
            return true;
        }

        // 이 명령을 사용할 수 있는 월드 제한
        java.util.List<String> allowedWorlds = sec.getStringList("allowed_worlds");
        if (allowedWorlds != null && !allowedWorlds.isEmpty()){
            String currentWorldName = p.getWorld().getName();
            if (!allowedWorlds.contains(currentWorldName)){
                p.sendMessage(ChatColor.RED + "이 월드에서는 랜덤야생을 사용할 수 없습니다.");
                return true;
            }
        }

        long now = System.currentTimeMillis();
        long allowAt = nextUse.getOrDefault(p.getUniqueId(), 0L);
        int cooldownSec = sec.getInt("cooldown_seconds", 60);
        if (now < allowAt){
            long remainMs = allowAt - now;
            long remainSec = (remainMs + 999)/1000;
            p.sendMessage(ChatColor.RED + "아직 대기 중입니다. " + remainSec + "초 후 사용 가능.");
            return true;
        }

        int minX = sec.getInt("min_x", -5000);
        int maxX = sec.getInt("max_x", 5000);
        int minZ = sec.getInt("min_z", -5000);
        int maxZ = sec.getInt("max_z", 5000);
        int attempts = sec.getInt("attempts", 30);

        java.util.List<String> blacklistNames = sec.getStringList("blacklist_blocks");
        Set<Material> blacklist = new HashSet<>();
        for (String n : blacklistNames){
            Material m = Material.matchMaterial(n);
            if (m != null) blacklist.add(m);
        }

        // 실제로 텔레포트될 월드 (target_world 설정, 없으면 현재 월드)
        String targetWorldName = sec.getString("target_world", p.getWorld().getName());
        World world = Bukkit.getWorld(targetWorldName);
        if (world == null){
            p.sendMessage(ChatColor.RED + "설정된 야생 월드를 찾을 수 없습니다: " + targetWorldName);
            return true;
        }

        Location safe = null;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < attempts && safe == null; i++){
            int x = rnd.nextInt(minX, maxX + 1);
            int z = rnd.nextInt(minZ, maxZ + 1);
            int y = world.getHighestBlockYAt(x, z);

            Location feet = new Location(world, x + 0.5, y, z + 0.5);
            Material blockAtFeet = world.getBlockAt(x, y - 1, z).getType();
            if (blacklist.contains(blockAtFeet)){
                continue;
            }
            safe = feet;
        }

        if (safe == null){
            p.sendMessage(ChatColor.RED + "안전한 위치를 찾지 못했습니다. 다시 시도해주세요.");
            return true;
        }

        // 사운드 설정
        ConfigurationSection soundSec = sec.getConfigurationSection("sound");
        final boolean soundEnabled;
        final String soundName;
        final float soundVolume;
        final float soundPitch;
        if (soundSec != null){
            soundEnabled = soundSec.getBoolean("enabled", true);
            soundName = soundSec.getString("name", "ENTITY_ENDERMAN_TELEPORT");
            soundVolume = (float) soundSec.getDouble("volume", 1.0D);
            soundPitch = (float) soundSec.getDouble("pitch", 1.0D);
        } else {
            soundEnabled = true;
            soundName = "ENTITY_ENDERMAN_TELEPORT";
            soundVolume = 1.0F;
            soundPitch = 1.0F;
        }

        // Countdown HUD -> TP
        final Player player = p;
        final Location dest = safe;
        final int cooldownVal = cooldownSec;
        final boolean sEnabled = soundEnabled;
        final String sName = soundName;
        final float sVol = soundVolume;
        final float sPitch = soundPitch;

        final int[] counter = {3};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!player.isOnline()) {
                task.cancel();
                return;
            }
            if (counter[0] > 0){
                String big = String.valueOf(counter[0]);
                Title title = Title.title(
                        net.kyori.adventure.text.Component.text("§e§l" + big),
                        net.kyori.adventure.text.Component.text("§7곧 야생으로 이동합니다..."),
                        Title.Times.times(Duration.ofMillis(50), Duration.ofMillis(600), Duration.ofMillis(150))
                );
                player.showTitle(title);
                player.sendActionBar(net.kyori.adventure.text.Component.text("§6§l" + big + " §7→ §f이동 준비"));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.5f);
                counter[0]--;
            } else {
                player.teleport(dest);
                player.setFallDistance(0);
                player.clearTitle();

                if (sEnabled){
                    try {
                        Sound s = Sound.valueOf(sName);
                        player.playSound(dest, s, sVol, sPitch);
                    } catch (IllegalArgumentException ignored){
                    }
                }

                player.sendActionBar(net.kyori.adventure.text.Component.text("§a이동 완료! 안전한 지형입니다."));
                player.sendMessage(ChatColor.GREEN + "야생으로 이동했습니다!");
                nextUse.put(player.getUniqueId(), System.currentTimeMillis() + cooldownVal * 1000L);
                task.cancel();
            }
        }, 0L, 20L);
        return true;
    }
}