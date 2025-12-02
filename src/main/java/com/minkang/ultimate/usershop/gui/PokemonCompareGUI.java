package com.minkang.ultimate.usershop.gui;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.util.PokemonDataParser;
import com.minkang.ultimate.usershop.util.PokemonDataParser.PokemonSummary;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 픽셀몬 포켓몬 데이터 아이템 두 개를 나란히 비교해서 보여주는 GUI.
 *
 * 왼쪽: 상점에 등록된 포켓몬
 * 오른쪽: 플레이어가 손에 들고 있는 포켓몬(있다면)
 */
public class PokemonCompareGUI implements InventoryHolder {

    private final Main plugin;
    private final Player viewer;
    private final ItemStack leftItem;
    private final ItemStack rightItem;
    private final Inventory inv;

    public PokemonCompareGUI(Main plugin, Player viewer, ItemStack leftItem, ItemStack rightItem) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.leftItem = leftItem == null ? null : leftItem.clone();
        this.rightItem = rightItem == null ? null : rightItem.clone();
        this.inv = Bukkit.createInventory(this, 54, Main.color("&b포켓몬 비교"));
        fill();
    }

    public void open() {
        viewer.openInventory(inv);
    }

    private void fill() {
        inv.clear();

        PokemonSummary left = PokemonDataParser.parse(leftItem);
        if (left == null) {
            viewer.sendMessage(Main.color("&c[유저상점] 이 아이템은 픽셀몬 포켓몬 데이터가 아닙니다."));
            viewer.closeInventory();
            return;
        }
        PokemonSummary right = PokemonDataParser.parse(rightItem);

        // 원본 아이템 표시
        inv.setItem(10, leftItem);

        if (rightItem != null && right != null) {
            inv.setItem(16, rightItem);
        }

        // 기본 정보 카드 (왼쪽)
        ItemStack leftInfo = new ItemStack(Material.BOOK);
        ItemMeta lm = leftInfo.getItemMeta();
        lm.setDisplayName(Main.color("&e왼쪽 포켓몬 정보"));
        List<String> ll = new ArrayList<>();
        ll.add(Main.color("&f종족: &b" + safe(left.speciesName)));
        if (left.level >= 0) ll.add(Main.color("&f레벨: &a" + left.level));
        if (!isEmpty(left.gender)) ll.add(Main.color("&f성별: &a" + left.gender));
        if (!isEmpty(left.nature)) ll.add(Main.color("&f성격: &a" + left.nature));
        if (!isEmpty(left.ability)) ll.add(Main.color("&f특성: &a" + left.ability));
        if (!isEmpty(left.ball)) ll.add(Main.color("&f볼: &a" + left.ball));
        if (!isEmpty(left.size)) ll.add(Main.color("&f크기: &a" + left.size));
        if (!isEmpty(left.hyperTrained)) ll.add(Main.color("&f왕관사용: &a" + left.hyperTrained));
        lm.setLore(ll);
        leftInfo.setItemMeta(lm);
        inv.setItem(28, leftInfo);

        // 스탯/IV/EV 카드 (왼쪽)
        ItemStack leftStats = new ItemStack(Material.PAPER);
        ItemMeta ls = leftStats.getItemMeta();
        ls.setDisplayName(Main.color("&e왼쪽 스탯 / 개체값 / 노력치"));
        List<String> lsLore = new ArrayList<>();
        if (!isEmpty(left.stats)) lsLore.add(Main.color("&f스탯: &a" + left.stats));
        if (!isEmpty(left.ivs)) lsLore.add(Main.color("&f개체값: &a" + left.ivs));
        if (!isEmpty(left.evs)) lsLore.add(Main.color("&f노력치: &a" + left.evs));
        if (!left.moves.isEmpty()) {
            lsLore.add(Main.color("&f기술 목록:"));
            for (String mv : left.moves) {
                lsLore.add(Main.color("&7- &b" + mv));
            }
        }
        ls.setLore(lsLore);
        leftStats.setItemMeta(ls);
        inv.setItem(37, leftStats);

        // 오른쪽 정보 (있을 때만)
        if (right != null) {
            ItemStack rightInfo = new ItemStack(Material.BOOK);
            ItemMeta rm = rightInfo.getItemMeta();
            rm.setDisplayName(Main.color("&e오른쪽 포켓몬 정보"));
            List<String> rl = new ArrayList<>();
            rl.add(Main.color("&f종족: &b" + safe(right.speciesName)));
            if (right.level >= 0) rl.add(Main.color("&f레벨: &a" + right.level));
            if (!isEmpty(right.gender)) rl.add(Main.color("&f성별: &a" + right.gender));
            if (!isEmpty(right.nature)) rl.add(Main.color("&f성격: &a" + right.nature));
            if (!isEmpty(right.ability)) rl.add(Main.color("&f특성: &a" + right.ability));
            if (!isEmpty(right.ball)) rl.add(Main.color("&f볼: &a" + right.ball));
            if (!isEmpty(right.size)) rl.add(Main.color("&f크기: &a" + right.size));
            if (!isEmpty(right.hyperTrained)) rl.add(Main.color("&f왕관사용: &a" + right.hyperTrained));
            rm.setLore(rl);
            rightInfo.setItemMeta(rm);
            inv.setItem(34, rightInfo);

            ItemStack rightStats = new ItemStack(Material.PAPER);
            ItemMeta rs = rightStats.getItemMeta();
            rs.setDisplayName(Main.color("&e오른쪽 스탯 / 개체값 / 노력치"));
            List<String> rsLore = new ArrayList<>();
            if (!isEmpty(right.stats)) rsLore.add(Main.color("&f스탯: &a" + right.stats));
            if (!isEmpty(right.ivs)) rsLore.add(Main.color("&f개체값: &a" + right.ivs));
            if (!isEmpty(right.evs)) rsLore.add(Main.color("&f노력치: &a" + right.evs));
            if (!right.moves.isEmpty()) {
                rsLore.add(Main.color("&f기술 목록:"));
                for (String mv : right.moves) {
                    rsLore.add(Main.color("&7- &b" + mv));
                }
            }
            rs.setLore(rsLore);
            rightStats.setItemMeta(rs);
            inv.setItem(43, rightStats);
        }

        // 닫기 버튼
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        cm.setDisplayName(Main.color("&c닫기"));
        List<String> cl = new ArrayList<>();
        cl.add(Main.color("&7클릭 시 이 창을 닫습니다."));
        cm.setLore(cl);
        close.setItemMeta(cm);
        inv.setItem(49, close);
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    public void onClick(InventoryClickEvent e) {
        e.setCancelled(true);
        int raw = e.getRawSlot();
        if (raw == 49) {
            viewer.closeInventory();
        }
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
