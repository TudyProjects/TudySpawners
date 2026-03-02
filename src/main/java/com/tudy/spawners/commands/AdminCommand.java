package com.tudy.spawners.commands;

import com.tudy.spawners.TudySpawners;
import com.tudy.spawners.menu.MenuManager;
import com.tudy.spawners.util.DropTable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AdminCommand implements CommandExecutor, TabExecutor {

    private final TudySpawners plugin;

    public AdminCommand(TudySpawners plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("tudyspawners.admin") && !sender.isOp()) {
            plugin.sendMessage(sender, "admin.no-permission");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            MenuManager.load(plugin);
            DropTable.load(plugin);
            plugin.sendMessage(sender, "admin.reload");
            return true;
        }

        if (args.length >= 3 && args[0].equalsIgnoreCase("give")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                plugin.sendMessage(sender, "admin.player-not-found");
                return true;
            }

            EntityType type;
            try {
                type = EntityType.valueOf(args[2].toUpperCase());
            } catch (Exception e) {
                plugin.sendMessage(sender, "admin.invalid-type");
                return true;
            }
            int amount = args.length >= 4 ? Integer.parseInt(args[3]) : 1;

            ItemStack item = new ItemStack(Material.SPAWNER, amount);
            ItemMeta meta = item.getItemMeta();

            // İsim Rengi
            meta.displayName(Component.text("Spawner").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));

            // Lore
            List<Component> lore = new ArrayList<>();
            lore.add(Component.translatable(type.translationKey()).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);

            NamespacedKey key = new NamespacedKey(plugin, "spawner_type");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, type.name());
            meta.addItemFlags(ItemFlag.values());

            item.setItemMeta(meta);

            target.getInventory().addItem(item);

            plugin.sendMessage(sender, "admin.give-success",
                    "%player%", target.getName(),
                    "%amount%", String.valueOf(amount),
                    "%type%", type.name());
            return true;
        }

        plugin.sendMessage(sender, "admin.give-usage");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("tudyspawners.admin")) return new ArrayList<>();

        if (args.length == 1) {
            return Arrays.asList("give", "reload");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return null;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Arrays.stream(EntityType.values())
                    .map(EntityType::name)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}