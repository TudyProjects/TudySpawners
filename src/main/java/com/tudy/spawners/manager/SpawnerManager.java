package com.tudy.spawners.manager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tudy.spawners.TudySpawners;
import com.tudy.spawners.database.DatabaseManager;
import com.tudy.spawners.model.StackedSpawner;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnerManager {

    private final TudySpawners plugin;
    private final DatabaseManager dbManager;
    private final Map<Location, StackedSpawner> spawners = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    public SpawnerManager(TudySpawners plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
    }

    public Map<Location, StackedSpawner> getSpawners() {
        return spawners;
    }

    public StackedSpawner getSpawner(Location loc) {
        return spawners.get(loc);
    }

    public void addSpawner(Location loc, StackedSpawner spawner) {
        spawners.put(loc, spawner);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveSpawnerToDB(spawner));
    }

    public void removeSpawner(Location loc) {
        spawners.remove(loc);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> deleteSpawnerFromDB(loc));
    }

    public void loadAllSpawners() {
        String query = "SELECT * FROM player_spawners";
        int count = 0;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String locStr = rs.getString("location_id");
                Location location = stringToLoc(locStr);

                if (location == null || location.getWorld() == null) {
                    plugin.getLogger().warning("Spawner konumu geçersiz veya dünya yüklü değil: " + locStr);
                    continue;
                }

                EntityType type = EntityType.valueOf(rs.getString("entity_type"));
                int stackSize = rs.getInt("stack_size");
                String storageJson = rs.getString("storage_data");

                // YENİ: Veritabanından XP'yi Oku
                int storedXp = 0;
                try {
                    storedXp = rs.getInt("stored_xp");
                } catch (SQLException ignored) {
                    // Eski veritabanı yapısında bu sütun yoksa 0 kabul ederiz
                }

                StackedSpawner spawner = new StackedSpawner(location, type, stackSize);
                spawner.setStoredXp(storedXp); // XP'yi spawner'a ekle

                if (storageJson != null && !storageJson.isEmpty()) {
                    Type storageType = new TypeToken<Map<Material, Long>>(){}.getType();
                    Map<Material, Long> storageMap = gson.fromJson(storageJson, storageType);
                    storageMap.forEach(spawner::addToStorage);
                    spawner.setDirty(false);
                }
                spawners.put(location, spawner);
                count++;
            }
            plugin.getLogger().info(count + " adet spawner veritabanından yüklendi.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Spawnerlar yüklenirken hata oluştu!");
            e.printStackTrace();
        }
    }

    public void saveAllSpawners(boolean force) {
        // YENİ: Kaydetme sorgusuna 'stored_xp' eklendi
        String query = "REPLACE INTO player_spawners (location_id, entity_type, stack_size, storage_data, stored_xp) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            int savedCount = 0;
            for (StackedSpawner spawner : spawners.values()) {
                if (!force && !spawner.isDirty()) continue;

                ps.setString(1, locToString(spawner.getLocation()));
                ps.setString(2, spawner.getType().name());
                ps.setInt(3, spawner.getStackSize());
                ps.setString(4, gson.toJson(spawner.getStorage()));
                ps.setInt(5, spawner.getStoredXp()); // XP verisini sorguya ekliyoruz

                ps.addBatch();
                spawner.setDirty(false);
                savedCount++;
            }
            if (savedCount > 0) {
                ps.executeBatch();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Spawnerlar kaydedilirken hata!");
            e.printStackTrace();
        }
    }

    private void saveSpawnerToDB(StackedSpawner spawner) {
        // YENİ: Kaydetme sorgusuna 'stored_xp' eklendi
        String query = "REPLACE INTO player_spawners (location_id, entity_type, stack_size, storage_data, stored_xp) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, locToString(spawner.getLocation()));
            ps.setString(2, spawner.getType().name());
            ps.setInt(3, spawner.getStackSize());
            ps.setString(4, gson.toJson(spawner.getStorage()));
            ps.setInt(5, spawner.getStoredXp()); // XP verisini sorguya ekliyoruz

            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Spawner eklenirken veritabanı hatası!");
            e.printStackTrace();
        }
    }

    private void deleteSpawnerFromDB(Location loc) {
        String query = "DELETE FROM player_spawners WHERE location_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, locToString(loc));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String locToString(Location loc) {
        if (loc == null || loc.getWorld() == null) return "";
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location stringToLoc(String str) {
        if (str == null || str.isEmpty()) return null;
        String[] parts = str.split(",");
        if (parts.length != 4) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }
}