package com.tudy.spawners.listeners;

import com.tudy.spawners.TudySpawners;
import com.tudy.spawners.manager.SpawnerManager;
import com.tudy.spawners.menu.MenuManager;
import com.tudy.spawners.menu.StorageMenu;
import com.tudy.spawners.model.StackedSpawner;
import com.tudy.spawners.util.DropTable;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MenuListener implements Listener {

    private final TudySpawners plugin;
    private final SpawnerManager spawnerManager;
    public static final Map<UUID, StackedSpawner> openedSpawners = new HashMap<>();

    public MenuListener(TudySpawners plugin, SpawnerManager spawnerManager) {
        this.plugin = plugin;
        this.spawnerManager = spawnerManager;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();

        StackedSpawner spawner = openedSpawners.get(player.getUniqueId());

        // 1. CONFIRM MENU
        if (title.equals(MenuManager.getMenuTitle("confirm-menu", null, 0))) {
            event.setCancelled(true);
            if (spawner == null) {
                player.closeInventory();
                return;
            }

            int slot = event.getSlot();

            if (slot == MenuManager.getSlot("confirm-menu.items.confirm")) {
                double totalMoney = 0;

                Map<Material, DropTable.DropItem> drops = DropTable.getDrops(spawner.getType());

                for (Map.Entry<Material, Long> storageEntry : spawner.getStorage().entrySet()) {
                    Material mat = storageEntry.getKey();
                    long amount = storageEntry.getValue();

                    if (drops.containsKey(mat)) {
                        DropTable.DropItem dropItem = drops.get(mat);
                        totalMoney += amount * dropItem.getPrice();
                    }
                }

                if (totalMoney > 0) {
                    TudySpawners.getEconomy().depositPlayer(player, totalMoney);
                    plugin.sendMessage(player, "menu.sell-success", "%money%", String.valueOf(totalMoney));
                }

                spawner.clearStorage();
                spawner.setDirty(true);
                player.closeInventory();
            }
            else if (slot == MenuManager.getSlot("confirm-menu.items.cancel")) {
                plugin.sendMessage(player, "menu.action-cancelled");
                player.closeInventory();
            }
            return;
        }

        // 2. MAIN MENU
        String expectedMainTitle = MenuManager.getMenuTitle("main-menu", spawner, 0);
        if (title.equals(expectedMainTitle)) {
            event.setCancelled(true);

            int slot = event.getSlot();

            if (slot == MenuManager.getSlot("main-menu.items.sell-button")) {
                if (spawner.getStorage().isEmpty()) {
                    plugin.sendMessage(player, "menu.sell-fail");
                    return;
                }
                MenuManager.openConfirmMenu(player);
            }
            else if (slot == MenuManager.getSlot("main-menu.items.storage-button")) {
                StorageMenu.open(player, spawner, 0);
            }
            // --- XP TOPLAMA BUTONU ---
            else if (slot == MenuManager.getSlot("main-menu.items.xp-button")) {
                int xpAmount = spawner.getStoredXp();

                if (xpAmount > 0) {
                    player.giveExp(xpAmount); // Sadece Ham XP verir, seviye atlatmaz

                    // Config'den mesajı çeker ve Actionbar/Chat ayarlarına göre iletir
                    plugin.sendMessage(player, "menu.xp-collected", "%xp%", String.valueOf(xpAmount));

                    spawner.clearXp();
                    MenuManager.openMainMenu(player, spawner); // Menüyü günceller
                } else {
                    // Config'den boş haznede verilecek uyarıyı çeker
                    plugin.sendMessage(player, "menu.xp-empty");
                }
            }
        }
    }
}