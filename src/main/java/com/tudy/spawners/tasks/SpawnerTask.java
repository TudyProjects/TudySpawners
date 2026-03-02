package com.tudy.spawners.tasks;

import com.tudy.spawners.TudySpawners;
import com.tudy.spawners.manager.SpawnerManager;
import com.tudy.spawners.model.StackedSpawner;
import com.tudy.spawners.util.DropTable;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.Map;
import java.util.Random;

public class SpawnerTask extends BukkitRunnable {

    private final SpawnerManager spawnerManager;
    private final Random random = new Random(); // Rastgele zar atışı için

    public SpawnerTask(SpawnerManager spawnerManager) {
        this.spawnerManager = spawnerManager;
    }

    @Override
    public void run() {
        double range = TudySpawners.getInstance().getConfig().getDouble("settings.activation-range", 16.0);
        int maxXpPerSpawner = TudySpawners.getInstance().getConfig().getInt("spawner-settings.max-xp-per-spawner", 200);

        // Config'den mob doğurma limitlerini alıyoruz
        int minMobs = TudySpawners.getInstance().getConfig().getInt("spawner-settings.min-mobs-per-spawn", 3);
        int maxMobs = TudySpawners.getInstance().getConfig().getInt("spawner-settings.max-mobs-per-spawn", 4);

        for (StackedSpawner spawner : spawnerManager.getSpawners().values()) {
            if (!spawner.getLocation().isChunkLoaded()) continue;

            if (spawner.getLocation().getWorld().getNearbyPlayers(spawner.getLocation(), range).isEmpty()) {
                continue;
            }

            int stackSize = spawner.getStackSize();

            // 1 spawner'ın bu döngüde kaç mob üreteceğini hesapla (örn: 3 veya 4)
            int mobsToSpawn = minMobs;
            if (maxMobs > minMobs) {
                mobsToSpawn = random.nextInt((maxMobs - minMobs) + 1) + minMobs;
            }

            // Toplam hesaplanacak zar sayısı (İstif Sayısı * O An Üretilen Mob Sayısı)
            int totalSpawns = stackSize * mobsToSpawn;

            int xpPerSpawn = DropTable.getXp(spawner.getType());

            if (xpPerSpawn > 0) {
                spawner.addXp(totalSpawns * xpPerSpawn, maxXpPerSpawner);
            }

            Map<Material, DropTable.DropItem> drops = DropTable.getDrops(spawner.getType());

            for (Map.Entry<Material, DropTable.DropItem> entry : drops.entrySet()) {
                Material dropMaterial = entry.getKey();
                DropTable.DropItem dropItem = entry.getValue();

                long totalDropAmount = 0;

                // Döngü artık toplam doğan mob sayısına göre çalışıyor
                for (int i = 0; i < totalSpawns; i++) {
                    if (random.nextDouble() * 100.0 <= dropItem.getChance()) {
                        int min = dropItem.getMin();
                        int max = dropItem.getMax();
                        int amount;
                        if (min == max) {
                            amount = min;
                        } else {
                            amount = random.nextInt((max - min) + 1) + min;
                        }
                        totalDropAmount += amount;
                    }
                }

                if (totalDropAmount > 0) {
                    spawner.addToStorage(dropMaterial, totalDropAmount);
                }
            }
        }
    }
}