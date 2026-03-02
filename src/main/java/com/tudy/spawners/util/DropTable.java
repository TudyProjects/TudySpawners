package com.tudy.spawners.util;

import com.tudy.spawners.TudySpawners;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import java.util.HashMap;
import java.util.Map;

public class DropTable {

    public static class DropItem {
        private final double price;
        private final double chance;
        private final int min;
        private final int max;

        public DropItem(double price, double chance, int min, int max) {
            this.price = price;
            this.chance = chance;
            this.min = min;
            this.max = max;
        }

        public double getPrice() { return price; }
        public double getChance() { return chance; }
        public int getMin() { return min; }
        public int getMax() { return max; }
    }

    private static final Map<EntityType, Map<Material, DropItem>> mobDrops = new HashMap<>();
    private static final Map<EntityType, Integer> mobXp = new HashMap<>(); // YENİ: Mob XP Değerleri

    public static void load(TudySpawners plugin) {
        mobDrops.clear();
        mobXp.clear();
        ConfigurationSection mobsSection = plugin.getConfig().getConfigurationSection("mobs");
        if (mobsSection == null) return;

        for (String key : mobsSection.getKeys(false)) {
            try {
                EntityType type = EntityType.valueOf(key.toUpperCase());

                // YENİ: Config'den mobun kendi XP'sini çek (Yoksa 0 sayar)
                int xp = mobsSection.getInt(key + ".xp", 0);
                mobXp.put(type, xp);

                ConfigurationSection dropsSection = mobsSection.getConfigurationSection(key + ".drops");

                if (dropsSection != null) {
                    Map<Material, DropItem> dropsMap = new HashMap<>();

                    for (String matKey : dropsSection.getKeys(false)) {
                        Material mat = Material.matchMaterial(matKey.toUpperCase());
                        if (mat != null) {
                            ConfigurationSection itemSection = dropsSection.getConfigurationSection(matKey);

                            if (itemSection != null) {
                                double price = itemSection.getDouble("price", 0.0);
                                double chance = itemSection.getDouble("chance", 100.0);
                                int min = itemSection.getInt("min", 1);
                                int max = itemSection.getInt("max", 1);

                                dropsMap.put(mat, new DropItem(price, chance, min, max));
                            } else {
                                double price = dropsSection.getDouble(matKey, 0.0);
                                dropsMap.put(mat, new DropItem(price, 100.0, 1, 1));
                            }
                        }
                    }
                    mobDrops.put(type, dropsMap);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Mob ayarı yüklenirken hata (Geçersiz isim): " + key);
            }
        }
    }

    public static Map<Material, DropItem> getDrops(EntityType type) {
        return mobDrops.getOrDefault(type, new HashMap<>());
    }

    // YENİ: Başka sınıflardan mobun XP miktarını öğrenmek için
    public static int getXp(EntityType type) {
        return mobXp.getOrDefault(type, 0);
    }
}