package com.tudy.spawners.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StackedSpawner {

    private final Location location;
    private EntityType type;
    private int stackSize;
    private final Map<Material, Long> storage;
    private int storedXp; // YENİ: XP Deposu
    private boolean dirty = false;

    public StackedSpawner(Location location, EntityType type, int stackSize) {
        this.location = location;
        this.type = type;
        this.stackSize = stackSize;
        this.storage = new ConcurrentHashMap<>();
        this.storedXp = 0;
    }

    public void addToStorage(Material material, long amount) {
        storage.merge(material, amount, Long::sum);
        this.dirty = true;
    }

    public void clearStorage() {
        this.storage.clear();
        this.dirty = true;
    }

    public boolean withdrawFromStorage(Material material, long amount) {
        long current = storage.getOrDefault(material, 0L);
        if (current < amount) return false;

        long remaining = current - amount;
        if (remaining == 0) {
            storage.remove(material);
        } else {
            storage.put(material, remaining);
        }
        this.dirty = true;
        return true;
    }

    // --- XP METOTLARI ---
    public int getStoredXp() {
        return storedXp;
    }

    public void setStoredXp(int storedXp) {
        this.storedXp = storedXp;
        this.dirty = true;
    }

    public void addXp(int amount, int maxPerSpawner) {
        int maxLimit = this.stackSize * maxPerSpawner; // İstif sayısına göre maksimum limit
        if (this.storedXp < maxLimit) {
            this.storedXp += amount;
            if (this.storedXp > maxLimit) {
                this.storedXp = maxLimit; // Sınırı aşarsa maksimuma sabitle
            }
            this.dirty = true;
        }
    }

    public void clearXp() {
        this.storedXp = 0;
        this.dirty = true;
    }
    // --------------------

    public void upgradeStack(int amount) {
        this.stackSize += amount;
        this.dirty = true;
    }

    public void decrementStack() {
        if (this.stackSize > 0) {
            this.stackSize--;
            this.dirty = true;
        }
    }

    public Location getLocation() { return location; }
    public EntityType getType() { return type; }
    public int getStackSize() { return stackSize; }
    public Map<Material, Long> getStorage() { return new HashMap<>(storage); }
    public boolean isDirty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }

    public void setType(EntityType type) {
        this.type = type;
        this.dirty = true;
    }
}