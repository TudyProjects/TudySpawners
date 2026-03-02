package com.tudy.spawners.listeners;

import com.tudy.spawners.TudySpawners;
import com.tudy.spawners.menu.StorageMenu;
import com.tudy.spawners.model.StackedSpawner;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorageListener implements Listener {

    private final TudySpawners plugin;

    public StorageListener(TudySpawners plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onStorageClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith(StorageMenu.STORAGE_MENU_TITLE)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        Player player = (Player) event.getWhoClicked();
        StackedSpawner spawner = MenuListener.openedSpawners.get(player.getUniqueId());

        if (spawner == null || event.getClickedInventory() == player.getInventory()) return;

        int currentPage = 0;
        try {
            String pageStr = event.getView().getTitle().replace(StorageMenu.STORAGE_MENU_TITLE, "");
            currentPage = Integer.parseInt(pageStr) - 1;
        } catch (NumberFormatException e) { return; }

        List<ItemStack> allItems = new ArrayList<>();
        for (Map.Entry<Material, Long> entry : spawner.getStorage().entrySet()) {
            long amount = entry.getValue();
            Material mat = entry.getKey();
            while (amount > 0) {
                int count = (int) Math.min(amount, 64);
                ItemStack item = new ItemStack(mat, count);
                allItems.add(item);
                amount -= count;
            }
        }

        int start = currentPage * 45;
        int end = Math.min(start + 45, allItems.size());

        if (clicked.getType() == Material.ARROW) {
            if (event.getSlot() == 50) StorageMenu.open(player, spawner, currentPage + 1);
            else if (event.getSlot() == 48 && currentPage > 0) StorageMenu.open(player, spawner, currentPage - 1);
        }

        else if (clicked.getType() == Material.DISPENSER) {
            boolean droppedAny = false;

            for (int i = start; i < end; i++) {
                ItemStack itemToDrop = allItems.get(i);
                player.getWorld().dropItemNaturally(player.getLocation(), itemToDrop);
                spawner.withdrawFromStorage(itemToDrop.getType(), itemToDrop.getAmount());
                droppedAny = true;
            }

            if (droppedAny) {
                // messages.yml -> menu.items-dropped-page
                plugin.sendMessage(player, "menu.items-dropped-page");
            } else {
                // messages.yml -> menu.items-dropped-fail
                plugin.sendMessage(player, "menu.items-dropped-fail");
            }

            StorageMenu.open(player, spawner, currentPage);
        }

        else if (event.getSlot() < 45 && clicked.getType() != Material.AIR) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(clicked);

            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                // messages.yml -> menu.inventory-full
                plugin.sendMessage(player, "menu.inventory-full");
            }

            spawner.withdrawFromStorage(clicked.getType(), clicked.getAmount());
            StorageMenu.open(player, spawner, currentPage);
        }
    }
}