package com.tudy.spawners.menu;

import com.tudy.spawners.TudySpawners;
import com.tudy.spawners.model.StackedSpawner;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StorageMenu {

    public static final String STORAGE_MENU_TITLE = ChatColor.DARK_GRAY + "Depo Yönetimi - Sayfa ";

    public static void open(Player player, StackedSpawner spawner, int page) {
        String title = MenuManager.getMenuTitle("storage-menu", spawner, page);
        Inventory inv = Bukkit.createInventory(null, 54, title);

        List<ItemStack> allItems = new ArrayList<>();
        List<String> loreFormat = MenuManager.getStorageLore();

        for (Map.Entry<Material, Long> entry : spawner.getStorage().entrySet()) {
            long amount = entry.getValue();
            Material mat = entry.getKey();
            while (amount > 0) {
                int count = (int) Math.min(amount, 64);
                ItemStack item = new ItemStack(mat, count);
                ItemMeta meta = item.getItemMeta();

                // FIX: Eşya ismini Türkçeleştir, Beyaz Yap, İtaliği Kaldır
                String itemName = TudySpawners.getInstance().getItemName(mat);
                meta.displayName(Component.text(itemName)
                        .color(NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false));

                List<Component> lore = new ArrayList<>();
                for (String line : loreFormat) {
                    String formatted = ChatColor.translateAlternateColorCodes('&', line.replace("%amount%", String.valueOf(entry.getValue())));
                    lore.add(Component.text(formatted));
                }
                meta.lore(lore);

                item.setItemMeta(meta);
                allItems.add(item);
                amount -= count;
            }
        }

        int start = page * 45;
        int end = Math.min(start + 45, allItems.size());
        for (int i = start; i < end; i++) {
            inv.setItem(i - start, allItems.get(i));
        }

        if (page > 0) {
            inv.setItem(48, MenuManager.createSimpleItem("storage-menu.items.previous-page"));
        }
        if (end < allItems.size()) {
            inv.setItem(50, MenuManager.createSimpleItem("storage-menu.items.next-page"));
        }
        inv.setItem(49, MenuManager.createSimpleItem("storage-menu.items.drop-all"));

        player.openInventory(inv);
    }
}