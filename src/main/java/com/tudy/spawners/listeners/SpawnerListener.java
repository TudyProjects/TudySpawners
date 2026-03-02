package com.tudy.spawners.listeners;

import com.tudy.spawners.TudySpawners;
import com.tudy.spawners.manager.SpawnerManager;
import com.tudy.spawners.menu.MenuManager;
import com.tudy.spawners.model.StackedSpawner;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SpawnerListener implements Listener {

    private final TudySpawners plugin;
    private final SpawnerManager spawnerManager;
    private final NamespacedKey key;

    public SpawnerListener(TudySpawners plugin, SpawnerManager spawnerManager) {
        this.plugin = plugin;
        this.spawnerManager = spawnerManager;
        this.key = new NamespacedKey(plugin, "spawner_type");
    }

    // --- PATLAMA KORUMASI (Creeper, TNT, Kristal) ---
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block block = it.next();
            if (block.getType() == Material.SPAWNER) {
                if (spawnerManager.getSpawner(block.getLocation()) != null) {
                    it.remove();
                }
            }
        }
    }

    // --- PATLAMA KORUMASI (Yatak, Respawn Anchor) ---
    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block block = it.next();
            if (block.getType() == Material.SPAWNER) {
                if (spawnerManager.getSpawner(block.getLocation()) != null) {
                    it.remove();
                }
            }
        }
    }

    @EventHandler
    public void onMobSpawn(SpawnerSpawnEvent event) {
        if (event.getSpawner() == null) return;
        if (spawnerManager.getSpawner(event.getSpawner().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSpawnerPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER) return;

        ItemStack item = event.getItemInHand();
        EntityType type = getSpawnerTypeFromItem(item);

        CreatureSpawner cs = (CreatureSpawner) event.getBlock().getState();
        cs.setSpawnedType(type);
        cs.update();

        StackedSpawner newSpawner = new StackedSpawner(event.getBlock().getLocation(), type, 1);
        spawnerManager.addSpawner(event.getBlock().getLocation(), newSpawner);

        sendMessage(event.getPlayer(), "spawner.placed");
    }

    @EventHandler
    public void onSpawnerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.SPAWNER) return;

        StackedSpawner spawner = spawnerManager.getSpawner(block.getLocation());
        if (spawner == null) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.SPAWNER) {
            EntityType itemType = getSpawnerTypeFromItem(item);
            if (itemType == spawner.getType()) {
                event.setCancelled(true);

                int limit = plugin.getConfig().getInt("settings.stack-limit", 50);
                if (spawner.getStackSize() >= limit) {
                    // Yeni replace destekli metod
                    plugin.sendMessage(player, "spawner.limit-reached", "%limit%", String.valueOf(limit));
                    return;
                }

                spawner.upgradeStack(1);
                item.setAmount(item.getAmount() - 1);

                plugin.sendMessage(player, "spawner.stacked", "%stack%", String.valueOf(spawner.getStackSize()));
                return;
            }
        }

        if (item != null && item.getType().name().endsWith("_SPAWN_EGG")) {
            String typeName = item.getType().name().replace("_SPAWN_EGG", "");
            try {
                EntityType newType = EntityType.valueOf(typeName);
                spawner.setType(newType);
                CreatureSpawner cs = (CreatureSpawner) block.getState();
                cs.setSpawnedType(newType);
                cs.update();
                plugin.sendMessage(player, "spawner.changed", "%type%", newType.name());
                return;
            } catch (Exception e) {}
        }

        event.setCancelled(true);
        MenuListener.openedSpawners.put(player.getUniqueId(), spawner);
        MenuManager.openMainMenu(player, spawner);
    }

    @EventHandler
    public void onSpawnerBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER) return;

        StackedSpawner spawner = spawnerManager.getSpawner(event.getBlock().getLocation());
        if (spawner == null) return;

        Player player = event.getPlayer();

        if (!player.hasPermission("tudyspawners.use")) {
            event.setCancelled(true);
            sendMessage(player, "admin.no-permission");
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();

        if (!hand.containsEnchantment(Enchantment.SILK_TOUCH)) {
            event.setCancelled(true);
            sendMessage(player, "spawner.silk-touch-required");
            return;
        }

        event.setExpToDrop(0);
        event.setDropItems(false);

        ItemStack dropItem = createSpawnerItem(spawner.getType());
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), dropItem);

        if (spawner.getStackSize() > 1) {
            event.setCancelled(true);
            spawner.decrementStack();
            plugin.sendMessage(player, "spawner.broken", "%stack%", String.valueOf(spawner.getStackSize()));
        } else {
            spawnerManager.removeSpawner(event.getBlock().getLocation());
            sendMessage(player, "spawner.broken-final");
        }
    }

    private void sendMessage(Player player, String key) {
        // Artık merkezi sistemi kullanıyor
        plugin.sendMessage(player, key);
    }

    private EntityType getSpawnerTypeFromItem(ItemStack item) {
        if (!item.hasItemMeta()) return EntityType.PIG;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (container.has(key, PersistentDataType.STRING)) {
            try {
                return EntityType.valueOf(container.get(key, PersistentDataType.STRING));
            } catch (Exception e) { return EntityType.PIG; }
        }

        if (meta instanceof BlockStateMeta bsm) {
            if (bsm.getBlockState() instanceof CreatureSpawner cs) {
                if (cs.getSpawnedType() != null) return cs.getSpawnedType();
            }
        }
        return EntityType.PIG;
    }

    private ItemStack createSpawnerItem(EntityType type) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Spawner").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.translatable(type.translationKey()).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, type.name());
        meta.addItemFlags(ItemFlag.values());

        item.setItemMeta(meta);
        return item;
    }
}