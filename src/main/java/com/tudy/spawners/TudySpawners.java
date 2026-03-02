package com.tudy.spawners;

import com.tudy.spawners.database.DatabaseManager;
import com.tudy.spawners.listeners.MenuListener;
import com.tudy.spawners.listeners.SpawnerListener;
import com.tudy.spawners.listeners.StorageListener;
import com.tudy.spawners.manager.SpawnerManager;
import com.tudy.spawners.tasks.SpawnerTask;
import com.tudy.spawners.commands.AdminCommand;
import com.tudy.spawners.util.DropTable;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class TudySpawners extends JavaPlugin {

    private DatabaseManager dbManager;
    private SpawnerManager spawnerManager;
    private static Economy econ = null;
    private static TudySpawners instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveResource("menus.yml", false);
        saveResource("messages.yml", false);

        if (!setupEconomy()) {
            getLogger().severe("Vault veya Ekonomi eklentisi bulunamadı! Eklenti kapanıyor.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        DropTable.load(this);

        try {
            dbManager = new DatabaseManager(
                    getConfig().getString("database.type", "sqlite"),
                    getConfig().getString("database.host"),
                    getConfig().getInt("database.port"),
                    getConfig().getString("database.name"),
                    getConfig().getString("database.user"),
                    getConfig().getString("database.password"),
                    getDataFolder()
            );
        } catch (Exception e) {
            getLogger().severe("Veritabanına bağlanılamadı: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        spawnerManager = new SpawnerManager(this, dbManager);
        com.tudy.spawners.menu.MenuManager.load(this);
        spawnerManager.loadAllSpawners();

        getServer().getPluginManager().registerEvents(new SpawnerListener(this, spawnerManager), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this, spawnerManager), this);
        getServer().getPluginManager().registerEvents(new StorageListener(this), this);

        if (getCommand("tudyspawner") != null) {
            AdminCommand adminCmd = new AdminCommand(this);
            getCommand("tudyspawner").setExecutor(adminCmd);
            getCommand("tudyspawner").setTabCompleter(adminCmd);
        }

        // --- YENİ EKLENEN: DİNAMİK ÜRETİM SÜRESİ KONTROLÜ ---
        // Config'den saniyeyi alıp 20 ile çarparak tick'e çeviriyoruz. Eğer ayar yoksa varsayılan 60 saniye (1200 tick) olur.
        long spawnInterval = getConfig().getLong("settings.spawn-interval", 60) * 20L;
        new SpawnerTask(spawnerManager).runTaskTimer(this, spawnInterval, spawnInterval);

        long interval = getConfig().getLong("settings.autosave-interval", 300) * 20L;
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            spawnerManager.saveAllSpawners(false);
        }, interval, interval);

        getLogger().info("TudySpawners aktif!");
    }

    @Override
    public void onDisable() {
        if (spawnerManager != null) spawnerManager.saveAllSpawners(true);
        if (dbManager != null) dbManager.close();
        getLogger().info("TudySpawners devre dışı.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() { return econ; }
    public static TudySpawners getInstance() { return instance; }

    // --- GELİŞMİŞ MESAJ SİSTEMİ (GET + SEND) ---

    // Mesajı configden çeker (Eski metod)
    public String getMessage(String key) {
        File msgFile = new File(getDataFolder(), "messages.yml");
        if (!msgFile.exists()) saveResource("messages.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(msgFile);

        if (!config.getBoolean("enabled", true)) return null;

        String prefix = getConfig().getString("prefix", "&8[&6TudySpawners&8] &r");
        String msg = config.getString(key);
        if (msg == null) return ChatColor.RED + "Mesaj bulunamadı: " + key;

        String ymlPrefix = config.getString("prefix", prefix);
        return ChatColor.translateAlternateColorCodes('&', ymlPrefix + msg);
    }

    // Mesajı oyuncuya gönderir (Chat veya Actionbar)
    public void sendMessage(CommandSender sender, String key) {
        String msg = getMessage(key); // Mesajı al
        if (msg == null) return;

        // Configden ayarları oku
        File msgFile = new File(getDataFolder(), "messages.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(msgFile);
        boolean sendToChat = config.getBoolean("settings.send-to-chat", true);
        boolean sendToActionBar = config.getBoolean("settings.send-to-actionbar", true);

        if (sender instanceof Player player) {
            // 1. Chat'e Gönder
            if (sendToChat) {
                player.sendMessage(msg);
            }
            // 2. Actionbar'a Gönder (Adventure API)
            if (sendToActionBar) {
                // Mesajdaki renk kodlarını (&a, §a) Actionbar'a uygun hale getirir
                player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(msg));
            }
        } else {
            // Konsol ise sadece düz mesaj at
            sender.sendMessage(msg);
        }
    }

    // Mesajın içine değişken (%money% vb.) yerleştirmek gerekirse kullanılacak yardımcı metod
    public void sendMessage(CommandSender sender, String key, String... placeholders) {
        String msg = getMessage(key);
        if (msg == null) return;

        // Placeholders: "key1", "value1", "key2", "value2" şeklinde gelir
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                msg = msg.replace(placeholders[i], placeholders[i + 1]);
            }
        }

        File msgFile = new File(getDataFolder(), "messages.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(msgFile);
        boolean sendToChat = config.getBoolean("settings.send-to-chat", true);
        boolean sendToActionBar = config.getBoolean("settings.send-to-actionbar", true);

        if (sender instanceof Player player) {
            if (sendToChat) player.sendMessage(msg);
            if (sendToActionBar) player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(msg));
        } else {
            sender.sendMessage(msg);
        }
    }

    public String getMobName(EntityType type) {
        File msgFile = new File(getDataFolder(), "messages.yml");
        if (!msgFile.exists()) saveResource("messages.yml", false);
        FileConfiguration config = YamlConfiguration.loadConfiguration(msgFile);

        String customName = config.getString("mob-names." + type.name());
        if (customName != null) return ChatColor.translateAlternateColorCodes('&', customName);

        String name = type.name().toLowerCase().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public String getItemName(Material material) {
        File msgFile = new File(getDataFolder(), "messages.yml");
        if (!msgFile.exists()) saveResource("messages.yml", false);
        FileConfiguration config = YamlConfiguration.loadConfiguration(msgFile);

        String customName = config.getString("item-names." + material.name());
        if (customName != null) return ChatColor.translateAlternateColorCodes('&', customName);

        String name = material.name().toLowerCase().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}