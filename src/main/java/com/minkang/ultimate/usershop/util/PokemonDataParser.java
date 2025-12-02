package com.minkang.ultimate.usershop.util;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PixelmonItem 플러그인이 만든 포켓몬 데이터 아이템의 로어를 파싱해서
 * 비교용 요약 정보를 뽑아내는 유틸리티.
 *
 * NBT를 직접 건드리지 않고, 로어 문자열만 기반으로 동작하므로
 * PixelmonItem / Pixelmon 버전에 크게 의존하지 않는다.
 */
public class PokemonDataParser {

    public static class PokemonSummary {
        public String rawName;
        public String speciesName;
        public int level = -1;
        public String gender = "";
        public String nature = "";
        public String ability = "";
        public String ball = "";
        public String size = "";
        public String stats = "";
        public String evs = "";
        public String ivs = "";
        public String hyperTrained = "";
        public List<String> moves = new ArrayList<>();
    }

    private static String strip(String s) {
        if (s == null) return "";
        return ChatColor.stripColor(s).trim();
    }

    public static boolean isPixelmonItem(ItemStack stack) {
        if (stack == null) return false;
        if (!stack.hasItemMeta()) return false;
        ItemMeta meta = stack.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return false;
        boolean hasLevel = false;
        boolean hasMoves = false;
        for (String line : lore) {
            String plain = strip(line);
            if (plain.startsWith("레벨") || plain.contains("레벨:")) {
                hasLevel = true;
            }
            if (plain.contains("기술 목록")) {
                hasMoves = true;
            }
        }
        return hasLevel && hasMoves;
    }

    public static PokemonSummary parse(ItemStack stack) {
        if (!isPixelmonItem(stack)) return null;
        ItemMeta meta = stack.getItemMeta();
        PokemonSummary s = new PokemonSummary();

        String dn = meta.hasDisplayName() ? ChatColor.stripColor(meta.getDisplayName()) : "";
        s.rawName = dn;
        // 예: [피카츄] 데이터
        int lb = dn.indexOf('[');
        int rb = dn.indexOf(']');
        if (lb >= 0 && rb > lb) {
            s.speciesName = dn.substring(lb + 1, rb).trim();
        } else {
            s.speciesName = dn.trim();
        }

        List<String> lore = meta.getLore();
        if (lore == null) lore = Collections.emptyList();

        boolean inMoves = false;
        for (String line : lore) {
            String plain = strip(line);
            if (plain.isEmpty()) continue;

            if (plain.contains("기술 목록")) {
                inMoves = true;
                continue;
            }
            if (plain.contains("--------------------")) {
                if (inMoves) {
                    // 기술 목록 종료
                    inMoves = false;
                }
                continue;
            }

            if (inMoves) {
                // "- 기술명" 형식
                String t = plain;
                if (t.startsWith("-")) t = t.substring(1).trim();
                if (!t.isEmpty()) s.moves.add(t);
                continue;
            }

            // key: value 형식 파싱
            String[] parts = plain.split(":", 2);
            String key = parts[0].trim();
            String value = parts.length > 1 ? parts[1].trim() : "";

            if (key.startsWith("레벨")) {
                try { s.level = Integer.parseInt(value.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
            } else if (key.startsWith("성별")) {
                s.gender = value;
            } else if (key.startsWith("성격")) {
                s.nature = value;
            } else if (key.startsWith("특성")) {
                s.ability = value;
            } else if (key.startsWith("볼")) {
                s.ball = value;
            } else if (key.startsWith("크기")) {
                s.size = value;
            } else if (key.startsWith("스탯")) {
                s.stats = value;
            } else if (key.startsWith("노력치")) {
                s.evs = value;
            } else if (key.startsWith("개체값")) {
                s.ivs = value;
            } else if (key.startsWith("왕관사용")) {
                s.hyperTrained = value;
            }
        }

        return s;
    }
}
