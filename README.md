# 📎 TudySpawners

An advanced **spawner management system** for Minecraft servers.  
Includes explosion protection, stack limits, advanced drop mechanics, and Vault economy integration.

![SpawnerGUI](https://github.com/user-attachments/assets/bd281098-10bf-4043-a5d1-572eb989d181)

## 🚀 Features

- **Explosion Protection** - Spawners are fully protected from TNT, Creeper, and other entity explosions.

- **Stack System** - Spawners can be stacked up to a limit of **32** in a single block to save space and reduce server lag.

- **Dynamic Spawn Time** - Optimized dynamic spawning system operating at a **25-second** interval.

- **Advanced Drop System** - Configure specific item drops for each spawner type.  
  - Set custom drop chances, minimum/maximum drop amounts, and item prices.

- **XP Collection & Notifications** - Automatically collects XP from spawned mobs.  
  - Sends customizable notifications to players via **Actionbar** or **Chat**.

- **Custom Icons & Localization** - Supports **Base64** head icons for customized UI and item displays.  
  - Includes native **Turkish** mob and item names built-in.

- **Vault Economy Integration** - Fully hooked into **Vault** to support economy-based features and drop pricing.

## ⚡ Commands

| Command | Description | Aliases |
|---------|-------------|---------|
| `/tudyspawners reload` | Reloads the configuration files. | `/tudyspawner reload` |
| `/tudyspawners give <player> <mob> [amount]` | Gives a specific spawner to a player. | `/tudyspawner give` |

## 🛡️ Permissions

| Permission | Description |
|------------|-------------|
| `tudyspawners.admin` | Grants access to all admin commands (`/tudyspawner reload`, `/tudyspawner give`). |
| `tudyspawners.use` | Allows the player to use TudySpawners. |

## ⚙️ Supported Forks

| Fork / Build | Support Status  |
|--------------|----------------|
| ✅ Paper     | Fully Supported |
| ✅ Purpur    | Fully Supported |
| ✅ Spigot    | Fully Supported |
