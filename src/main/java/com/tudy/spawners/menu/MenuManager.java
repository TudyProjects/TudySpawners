package com.tudy.spawners.menu;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.tudy.spawners.TudySpawners;
import com.tudy.spawners.model.StackedSpawner;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MenuManager {

    private static FileConfiguration menuConfig;
    private static File menuFile;

    public static void load(JavaPlugin plugin) {
        menuFile = new File(plugin.getDataFolder(), "menus.yml");
        if (!menuFile.exists()) {
            plugin.saveResource("menus.yml", false);
        }
        menuConfig = YamlConfiguration.loadConfiguration(menuFile);
    }

    public static String getMenuTitle(String menuName, StackedSpawner spawner, int page) {
        String title = menuConfig.getString(menuName + ".title", "Menu");
        if (spawner != null) {
            String mobName = TudySpawners.getInstance().getMobName(spawner.getType());
            title = title.replace("%type%", mobName)
                    .replace("%stack%", String.valueOf(spawner.getStackSize()))
                    .replace("%page%", String.valueOf(page + 1));
        }
        return color(title);
    }

    public static void openMainMenu(Player player, StackedSpawner spawner) {
        String title = getMenuTitle("main-menu", spawner, 0);
        int size = menuConfig.getInt("main-menu.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, title);

        inv.setItem(getSlot("main-menu.items.sell-button"), createItemFromConfig("main-menu.items.sell-button", spawner));
        inv.setItem(getSlot("main-menu.items.storage-button"), createItemFromConfig("main-menu.items.storage-button", spawner));
        inv.setItem(getSlot("main-menu.items.xp-button"), createItemFromConfig("main-menu.items.xp-button", spawner));

        ItemStack infoItem = createItemFromConfig("main-menu.items.info-icon", spawner);
        ItemMeta meta = infoItem.getItemMeta();
        List<String> lore = meta.getLore();

        if (lore != null) {
            String listFormat = menuConfig.getString("main-menu.storage-format", "&7- %item%: %amount%");

            for (Map.Entry<Material, Long> entry : spawner.getStorage().entrySet()) {
                String itemName = TudySpawners.getInstance().getItemName(entry.getKey());
                String line = listFormat
                        .replace("%item%", itemName)
                        .replace("%amount%", String.valueOf(entry.getValue()));
                lore.add(color(line));
            }

            if (spawner.getStorage().isEmpty()) {
                lore.add(ChatColor.RED + "Depo Boş");
            }
            meta.setLore(lore);
            infoItem.setItemMeta(meta);
        }
        inv.setItem(getSlot("main-menu.items.info-icon"), infoItem);

        fillBackground(inv);
        player.openInventory(inv);
    }

    public static void openConfirmMenu(Player player) {
        String title = getMenuTitle("confirm-menu", null, 0);
        int size = menuConfig.getInt("confirm-menu.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, title);

        inv.setItem(getSlot("confirm-menu.items.confirm"), createItemFromConfig("confirm-menu.items.confirm", null));
        inv.setItem(getSlot("confirm-menu.items.cancel"), createItemFromConfig("confirm-menu.items.cancel", null));
        inv.setItem(getSlot("confirm-menu.items.info"), createItemFromConfig("confirm-menu.items.info", null));

        fillBackground(inv);
        player.openInventory(inv);
    }

    public static int getSlot(String path) {
        return menuConfig.getInt(path + ".slot");
    }

    public static List<String> getStorageLore() {
        return menuConfig.getStringList("storage-menu.item-lore");
    }

    public static ItemStack createItemFromConfig(String path, StackedSpawner spawner) {
        ConfigurationSection section = menuConfig.getConfigurationSection(path);

        ItemStack item = new ItemStack(Material.STONE);

        if (spawner != null && path.endsWith("info-icon")) {
            String iconValue = menuConfig.getString("mob-icons." + spawner.getType().name());

            if (iconValue != null) {
                if (iconValue.length() > 30) {
                    item = getHead(iconValue);
                } else {
                    Material mat = Material.matchMaterial(iconValue);
                    if (mat != null) item = new ItemStack(mat);
                }
            } else {
                item = getDefaultMaterial(section);
            }
        } else {
            item = getDefaultMaterial(section);
        }

        ItemMeta meta = item.getItemMeta();

        if (section.contains("name")) {
            String name = color(section.getString("name"));
            if (spawner != null) {
                String mobName = TudySpawners.getInstance().getMobName(spawner.getType());
                int maxLimit = spawner.getStackSize() * TudySpawners.getInstance().getConfig().getInt("spawner-settings.max-xp-per-spawner", 200);

                name = name.replace("%type%", mobName)
                        .replace("%stack%", String.valueOf(spawner.getStackSize()))
                        .replace("%xp%", String.valueOf(spawner.getStoredXp()))
                        .replace("%max_xp%", String.valueOf(maxLimit));
            }
            meta.setDisplayName(name);
        }

        if (section.contains("lore")) {
            List<String> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) {
                String processedLine = color(line);
                if (spawner != null) {
                    String mobName = TudySpawners.getInstance().getMobName(spawner.getType());
                    int maxLimit = spawner.getStackSize() * TudySpawners.getInstance().getConfig().getInt("spawner-settings.max-xp-per-spawner", 200);

                    processedLine = processedLine.replace("%type%", mobName)
                            .replace("%stack%", String.valueOf(spawner.getStackSize()))
                            .replace("%location%", spawner.getLocation().getBlockX() + "," + spawner.getLocation().getBlockY() + "," + spawner.getLocation().getBlockZ())
                            .replace("%xp%", String.valueOf(spawner.getStoredXp()))
                            .replace("%max_xp%", String.valueOf(maxLimit));
                }
                lore.add(processedLine);
            }
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack getDefaultMaterial(ConfigurationSection section) {
        if (section == null) return new ItemStack(Material.STONE);
        Material mat = Material.matchMaterial(section.getString("material", "STONE"));
        if (mat == null) mat = Material.STONE;
        return new ItemStack(mat);
    }

    private static ItemStack getHead(String base64) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (base64 == null || base64.isEmpty()) return head;

        try {
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            UUID uuid = UUID.nameUUIDFromBytes(base64.getBytes(StandardCharsets.UTF_8));
            PlayerProfile profile = Bukkit.createProfile(uuid, "TudySkin");
            profile.setProperty(new ProfileProperty("textures", base64));
            meta.setPlayerProfile(profile);
            head.setItemMeta(meta);
        } catch (Exception e) {
            Bukkit.getLogger().warning("Kafa olusturulamadi: " + e.getMessage());
        }

        return head;
    }

    public static ItemStack createSimpleItem(String path) {
        return createItemFromConfig(path, null);
    }

    private static void fillBackground(Inventory inv) {
        String matName = menuConfig.getString("fill-material", "GRAY_STAINED_GLASS_PANE");
        if (matName.equalsIgnoreCase("AIR")) return;

        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.GRAY_STAINED_GLASS_PANE;

        ItemStack filler = new ItemStack(mat);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                inv.setItem(i, filler);
            }
        }
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}