<div align="center">

[![Paper](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/paper_vector.svg)](https://papermc.io/)
[![Purpur](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/purpur_vector.svg)](https://purpurmc.org/)

# 🔥 FireBallWand

### Advanced Administration, Moderation & Custom Items

**One plugin. Multiple possibilities. Complete control.**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21%2B-47A248?style=flat-square&logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur-8A63D2?style=flat-square)](https://papermc.io/)
[![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-5865F2?style=flat-square)](#license)
</div>

---

## 📖 About

**FireBallWand** is a modular Minecraft plugin designed to bring administration tools, moderation features, essential player commands and custom items together in one versatile solution.

Whether you're managing a Survival server, a private SMP or an event-based community, FireBallWand provides the tools needed to manage your players, moderate discreetly and customize your gameplay experience.

From an advanced **Vanish system** and **Staff Mode** to **homes**, a **spawn**, teleportation requests, private messaging, a built-in **economy module** and powerful custom items, every feature is designed to be configurable and easy to use.

### ✨ Main Highlights

- 🛡️ Complete Staff Mode with a safely stored inventory and a full moderation toolkit.
- 👻 Advanced hierarchical Vanish system that leaves no trace in commands or tab-complete.
- ⭐ God Mode and flight commands with configurable behavior.
- 🏠 Home system with a menu, plus a configurable spawn.
- 💥 Custom weapons, armor and fun items.
- 💬 Chat formatting, private messaging and custom join/leave messages.
- 🧭 Interactive teleportation request system.
- 💰 Built-in economy with payments, balance management, a leaderboard and Vault integration.
- 🎒 Automatic and configurable Starter Kit.
- 📂 Separate YAML configuration files for each module.
- 🔌 Optional integrations with popular plugins.

---

## 🛡️ Staff Features

### Staff Mode — `/staff`

A dedicated moderation toolkit that allows staff members to moderate players without switching to Creative Mode.

**How it works:**

- Toggle Staff Mode with `/staff`.
- Your real inventory is saved, then replaced by your moderation items.
- Execute `/staff` again to leave Staff Mode and get everything back.
- Each item is only provided when the corresponding permission is available.

**Available tools:**

| Item | Function |
|:---|:---|
| 👻 Vanish Item | Toggle your Vanish status |
| ❄️ FreezeWand | Freeze or unfreeze targeted players |
| ⭐ God Mode Item | Toggle God Mode |
| 🧭 Teleport Compass | Teleport to the next online player |
| 🪶 Fly Feather | Toggle flight |

**Inventory safety:**

- Your inventory (items, armor, offhand and optionally XP) is written to disk **before** being cleared. If that fails, Staff Mode simply doesn't start.
- It is restored when you leave Staff Mode, disconnect, die (at respawn) or when the server stops.
- After a server crash, it is restored automatically when you reconnect.
- Staff items can't be dropped, stored in chests or moved to the offhand, and never drop on death.

**Protections while in Staff Mode (each one can be toggled):**

- No breaking or placing blocks.
- No picking up or dropping items.
- No hitting players (the FreezeWand still works).

**Inspection tools:**

- 🔍 **Inventory viewer:** right-click a player to see their inventory live. Read-only by default, editing needs a separate permission.
- 🤫 **Silent containers:** while vanished, open chests, barrels, hoppers, furnaces, shulker boxes and more without any sound or animation (read-only). You briefly enter Spectator Mode and get your previous game mode back when you close the container.

Item names, materials, slots and messages are configurable, and the entire module can be disabled through `Staff.yml`.

### ⭐ God Mode

**Commands:** `/godmod`, `/ungodmod`

An invulnerability system for administrators and staff members.

- Cancels incoming damage and hunger depletion while active.
- The void and `/kill` can still affect players.
- Can also be activated through the Staff Mode item.
- Includes short aliases: `/gm` and `/ugm`.

### 🪶 Fly

**Commands:** `/fly [player]`, `/fly speed <1-10> [player]`

- Toggle flight for yourself or another player.
- Adjustable flight speed, with a configurable maximum.
- Turning flight off in mid-air ignores the fall damage that follows.

### 👻 Advanced Hierarchical Vanish

An advanced Vanish system designed for discreet staff moderation.

**Rank hierarchy:**

- Higher staff ranks can see lower-ranked vanished players.
- Lower staff ranks cannot see higher-ranked vanished players.
- Visibility is managed through permission-based levels.

**Discretion:**

- Toggling Vanish sends the same message a real disconnect or connect would (vanilla, custom or hidden, following your join/leave message settings), only to players who can't see that staff member.
- Join and quit messages are hidden while vanished.
- Vanished players' names are removed from the auto-complete of **every command**, including other plugins' commands.
- `/tpa`, `/tpahere`, `/msg`, `/starterkit` and `/fly` answer "player not found" for a vanished player the sender isn't allowed to see.
- Vanished players are excluded from the server-list player count and hover player list.
- TAB integration hides vanished players from unauthorized viewers.
- Persistent ActionBar reminder.

**Persistence and extras:**

- Vanish status is preserved after reconnecting.
- Vanished players cannot pick up items, and KickArmor never kicks them.
- Double-sneak to switch to Spectator Mode and return to the previous game mode.
- `/vanish list` displays the vanished players you are allowed to see.
- Chat protection prevents messages from being sent unless they end with `/`.

All Vanish settings, permission levels and messages can be customized in `Vanish.yml`.

---

## 🏠 Essentials

### Homes — `/home`, `/sethome`, `/delhome`

A home system with a clean in-game menu.

- Up to **5 homes** per player, controlled by permissions.
- `/home` or `/home list` opens a menu: left-click to teleport, right-click to delete.
- Tab-complete for home names, with safe names (letters, numbers, `-` and `_`).
- Configurable teleport delay (cancelled on movement or damage), optional cooldown and bypass permissions.
- Homes are loaded before the player joins and saved asynchronously, only when something changed.
- Teleports load the destination chunk without blocking the server.
- Homes from the old Home plugin are imported automatically on first start (the original file is kept as a backup).

All settings and messages are managed through `Home.yml`.

### Spawn — `/spawn`, `/setspawn`

- Set the server spawn with `/setspawn`.
- Configurable delay and cooldown, with bypass permissions.
- Teleport players to spawn on their first join, on every join, or never.
- Optionally respawn players at spawn after death (a valid bed or respawn anchor keeps priority).
- The spawn of the old NewSpawn plugin is imported automatically.

All settings and messages are managed through `Spawn.yml`.

---

## 💬 Chat & Communication

### Chat formatting

- Permission-based color access.
- Standard Minecraft color codes using `&`.
- Hexadecimal colors using `&#RRGGBB`.
- Rank prefixes and suffixes through LuckPerms.
- ItemsAdder glyph support for custom rank icons in chat and custom join/leave messages.
- PlaceholderAPI support in chat formatting and custom join/leave messages.
- In join/leave messages, PlaceholderAPI is parsed before ItemsAdder font images so prefixes using `%img_<name>%` and `:namespace:icon:` can be resolved.

### Private messaging

- `/msg` — Send a private message.
- `/reply` — Reply to the last private conversation.

### Welcome & join/leave messages

- Configurable first-join welcome message.
- Customizable join and leave messages: keep the vanilla ones, use your own text (with prefix and suffix placeholders) or hide them.
- A dedicated message for brand-new players.
- Optional LuckPerms prefix/suffix formatting with PlaceholderAPI and ItemsAdder glyph conversion.

---

## 💰 Economy & Money Module

FireBallWand includes a built-in economy module for player balances, transactions and server-wide economy integration.

### Player commands

| Command | Description | Permission |
|:---|:---|:---|
| `/money` | Display your balance | `fireballwand.money.use` |
| `/money <player>` | Display a cached player's balance | `fireballwand.money.use` |
| `/pay <player> <amount>` | Send money to an online player | `fireballwand.money.pay` |
| `/moneytop [page]` | Display the richest players | `fireballwand.money.top` |

Aliases: `/balance` and `/bal` for `/money`; `/baltop` for `/moneytop`.

### Administration

| Command | Description | Permission |
|:---|:---|:---|
| `/eco give <player> <amount>` | Add money to a player's balance | `fireballwand.money.admin` |
| `/eco take <player> <amount>` | Remove money from a player's balance | `fireballwand.money.admin` |
| `/eco set <player> <amount>` | Set a player's balance | `fireballwand.money.admin` |
| `/money reload` | Reload `Money.yml` | `fireballwand.money.admin` |

### Storage & Vault

- The default `internal` mode stores balances by player UUID in `plugins/FireBallWand/data/money.yml`.
- `Money.yml` controls the economy mode, currency names, starting balance, transaction limits, leaderboard pagination/cache and messages.
- When Vault is installed and the internal economy is active, FireBallWand registers its own Vault Economy provider. Other Vault-compatible plugins can then use the same FireBallWand balances.
- Set `mode: vault` in `Money.yml` to delegate balance operations to an external Vault Economy provider. If no external provider is found, FireBallWand falls back to its internal economy.
- `mode: auto` keeps the internal FireBallWand balances rather than silently switching to an external provider.

### Legacy balance migration

The Money module can import data from the following sources:

- The previous MainAPI DataStore, including YAML or MySQL when the old `MainAPI` and its DataStore are available at module startup.
- Legacy `Main` / `MainPlugin` YAML data files.
- The old standalone `Money` plugin YAML files.
- Essentials / EssentialsX `userdata/<UUID>.yml` files.
- A manually supplied `plugins/FireBallWand/migration-source.yml` file using one of the supported legacy YAML formats.

Imported balances are stored in `plugins/FireBallWand/data/money.yml` and matched by UUID. The previous FireBallWand root-level file is imported first, followed by MainAPI and the other supported sources. Multiple sources can be combined; valid balances already present in the new target always take priority.

**Migration safety:**

- Existing installations are automatically migrated from the old root-level `plugins/FireBallWand/money-data.yml` file into `plugins/FireBallWand/data/money.yml`.
- A `money-data.yml.pre-migration.bak` backup is created beside the old file; the original is retained and is not automatically deleted.
- Valid balances already present in `data/money.yml` are never overwritten. Missing UUIDs can be merged from legacy sources, including when the destination is partially populated.
- Other YAML source files are not deleted. A `.pre-migration.bak` backup is created before importing a YAML source.
- MainAPI DataStore imports require the old API and DataStore to be initialized and accessible to FireBallWand before the Money module starts.
- If no new `Money.yml` exists yet, compatible legacy settings in `plugins/Main/money/config.yml` (or matching MainPlugin folder names) are copied into the new configuration.
- Back up the complete `plugins/` directory before the first migration and verify balances in-game before removing old data or plugins.

Enable or disable the module in `config.yml`:

```yaml
modules:
  money:
    enabled: true
```

Economy settings are stored separately in `Money.yml`.

---

## 🧭 Teleportation Request System

A configurable TPA system with interactive chat controls.

**Commands:**

- `/tpa` — Request to teleport to another player.
- `/tpahere` — Request another player to teleport to you.
- `/tpaccept` — Accept a teleport request.
- `/tpdeny` — Deny a teleport request.
- `/tpacancel` — Cancel your pending request.

**Features:**

- Clickable Accept and Deny buttons directly in chat.
- Configurable teleportation warmup.
- Movement cancels the teleportation countdown.
- Damage cancels the teleportation countdown.
- Post-teleport invincibility protection.
- ActionBar countdown and protection messages.
- Configurable cooldown management.

Cooldown data uses asynchronous file operations to reduce unnecessary synchronous disk activity.

All settings are managed through `Tpa.yml`.

---

## 🎒 Starter Kit

**Command:** `/starterkit`

Give new players everything they need to start their adventure.

**Default kit includes:**

- Enchanted iron armor.
- Diamond tools.
- Food and survival consumables.

**Features:**

- Automatically granted on a player's first join.
- Manual claiming through `/starterkit`.
- Configurable claim cooldowns.
- Dedicated cooldown bypass permissions.
- Customizable kit configuration through `StartKit.yml`.

---

## 💥 Custom Items & Admin Arsenal

FireBallWand also includes a collection of powerful custom items designed for administration, testing and fun events.

| Item | Description |
|:---|:---|
| 🔥 **FireWand** | Launches explosive fireballs. |
| 👢 **KickArmor** | Automatically knocks back nearby players who get too close. |
| ❄️ **FreezeWand** | Freezes targeted players in place. |
| ⚡ **WinCharge Boosted** | A powerful explosive charge with x3 boosted power. |
| ☠️ **Potion of Death** | A deadly splash potion that bypasses Creative Mode and handles Keep Inventory. |
| ❄️ **Cursed Snowball** | Applies negative effects (slowness, blindness, nausea by default) to the player it hits. |
| ⬆️ **Sword Up** | The player you hit gets Levitation. |
| 🧊 **Ice Boots** | The wearer slides on any full block as if it were ice. |
| ⚔️ **Cheat Sword** | An overpowered custom sword. |
| 🔨 **Cheat Mace** | An enhanced custom mace. |
| 💎 **Diamond Cheat Armor** | A complete diamond armor set with extreme attributes and enchantments. |
| 🛡️ **Netherite Cheat Armor** | A complete netherite armor set with extreme attributes and enchantments. |

- Each fun item has its own dedicated give permission.
- OPs and players with the matching bypass permission are immune to the Cursed Snowball and the Sword Up (configurable).
- Ice Boots only send fake ice to the wearer: the world is never modified and nobody else sees anything.
- Certain equipment includes level 255+ enchantments and custom attributes.

All custom item settings are managed through `CustomCheat.yml`.

---

## ⚙️ Configuration System

FireBallWand uses a modular YAML configuration architecture.

Each feature has its own dedicated configuration file, making customization and maintenance easier.

| Configuration file | Purpose |
|:---|:---|
| `config.yml` | General settings, module toggles, chat, private messages and join/leave messages |
| `Money.yml` | Economy mode, currency names, starting balance, transaction limits, messages and leaderboard settings |
| `Staff.yml` | Staff Mode, protections, moderation items, God Mode and Fly |
| `Vanish.yml` | Vanish hierarchy, visibility and messages |
| `Home.yml` | Homes: limits, delay, cooldown and messages |
| `Spawn.yml` | Spawn: join and respawn behavior, delay, cooldown and messages |
| `CustomCheat.yml` | Custom weapons, armor and special items |
| `StartKit.yml` | Starter Kit contents and cooldowns |
| `Tpa.yml` | Teleportation requests and protection settings |

Messages in `Home.yml` and `Spawn.yml` accept both MiniMessage and `&` color codes.

**Reload every configuration file:**

```text
/fw reload
```

Permission: `fireballwand.admin`

`/fw reload` reloads the Money module as part of the plugin's global reload process. `/money reload` reloads only `Money.yml`.

---

## 🔌 Optional Integrations

FireBallWand works without additional plugins.

The following integrations can unlock additional functionality:

| Plugin | Integration |
|:---|:---|
| [LuckPerms](https://luckperms.net/) | Rank prefixes, suffixes and permissions |
| [ItemsAdder](https://itemsadder.com/) | Custom glyphs in chat and join/leave messages |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Placeholder support in chat and custom messages |
| [TAB](https://modrinth.com/plugin/tab-was-taken) | Vanish compatibility with the tab list |
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | Economy API bridge for plugins using FireBallWand's internal balances |

All integrations are optional. Vault integration requires Vault to be installed; the Money module's internal economy itself does not require an external economy plugin.

---

## 📥 Installation

### Requirements

- Minecraft Java Edition 1.21 or newer, subject to the release's compatibility.
- Paper or Purpur server.
- Java version required by your Minecraft server.

### Installation steps

1. Download the latest FireBallWand release.
2. Stop your Minecraft server.
3. Place `FireBallWand.jar` inside the `plugins/` directory.
4. Start your server.
5. The plugin will generate its configuration files automatically.
6. Customize the YAML files according to your needs.

**Optional:** Install LuckPerms, ItemsAdder, PlaceholderAPI, TAB and/or Vault to enable their respective integrations.

> **Using TAB?** Assign the `tab.seevanished` permission to staff members who should be able to see vanished players in the tab list.

> **Coming from the old Home or NewSpawn plugins?** Your homes and spawn are imported on the first start. Remove the old plugins afterwards to avoid duplicate commands.

> **Coming from an older economy setup?** Make a full backup of `plugins/` first. Keep the old economy source files until you have confirmed that the migrated balances are correct. The Money module imports balances during startup when a supported source is available.

---

## 📋 Commands & Permissions

### Administration & Moderation

| Command | Description | Permission |
|:---|:---|:---|
| `/staff` | Toggle Staff Mode | `fireballwand.staff` |
| `/godmod` / `/gm` | Enable God Mode | `fireballwand.god` |
| `/ungodmod` / `/ugm` | Disable God Mode | `fireballwand.god` |
| `/fly [player]` | Toggle flight | `fireballwand.fly` (`.fly.others` for other players) |
| `/fly speed <1-10> [player]` | Set the flight speed | `fireballwand.fly.speed` |
| `/vanish` / `/fv` | Toggle your Vanish status | `fireballwand.vanish.level.1` |
| `/vanish <player>` | Toggle another player's Vanish | `fireballwand.vanish.others` |
| `/vanish list` | Display vanished players | `fireballwand.vanish.list` |

### Homes & Spawn

| Command | Description | Permission |
|:---|:---|:---|
| `/home [name|list]` | Teleport to a home or open the menu | `fireballwand.home.use` |
| `/sethome [name]` | Set a home at your position | `fireballwand.home.set` |
| `/delhome [name]` | Delete one of your homes | `fireballwand.home.delete` |
| `/spawn` | Teleport to the spawn | `fireballwand.spawn.use` |
| `/setspawn` | Set the spawn at your position | `fireballwand.spawn.set` |

Everyone can use these commands by default. The number of homes is controlled by `fireballwand.home.max.1` to `fireballwand.home.max.5`.

### Utility & Communication

| Command | Description | Permission |
|:---|:---|:---|
| `/starterkit` | Claim the Starter Kit | `fireballwand.starterkit` |
| `/tpa <player>` | Request teleportation | `fireballwand.tpa` |
| `/tpahere <player>` | Invite a player to teleport to you | `fireballwand.tpahere` |
| `/tpaccept` | Accept a request | `fireballwand.tpa` |
| `/tpdeny` | Deny a request | `fireballwand.tpa` |
| `/tpacancel` | Cancel a request | `fireballwand.tpa` |
| `/msg <player>` | Send a private message | `fireballwand.msg` |
| `/reply` | Reply to a private message | `fireballwand.reply` |

### Economy

| Command | Description | Permission |
|:---|:---|:---|
| `/money` / `/balance` / `/bal` | View your balance | `fireballwand.money.use` |
| `/money <player>` | View another cached player's balance | `fireballwand.money.use` |
| `/pay <player> <amount>` | Pay an online player | `fireballwand.money.pay` |
| `/moneytop [page]` / `/baltop` | View the richest players | `fireballwand.money.top` |
| `/eco give <player> <amount>` | Add money | `fireballwand.money.admin` |
| `/eco take <player> <amount>` | Remove money | `fireballwand.money.admin` |
| `/eco set <player> <amount>` | Set a balance | `fireballwand.money.admin` |
| `/money reload` | Reload `Money.yml` | `fireballwand.money.admin` |

### Custom Items

| Command | Description | Permission |
|:---|:---|:---|
| `/firewand` | Get the FireWand | `fireballwand.firewand` |
| `/kickarmor` | Get the KickArmor | `fireballwand.kickarmor` |
| `/freezewand` | Get the FreezeWand | `fireballwand.freezewand` |
| `/wincharge_boosted` | Get the boosted WinCharge | `fireballwand.wincharge_boosted` |
| `/potionofdeath` / `/pod` | Get the Potion of Death | `fireballwand.potion-of-death` |
| `/negativesnowball` / `/nsnow` | Get the Cursed Snowballs | `fireballwand.negativesnowball` |
| `/swordup` | Get the Sword Up | `fireballwand.swordup` |
| `/iceboots` | Get the Ice Boots | `fireballwand.iceboots` |
| `/cheatsword` | Get the Cheat Sword | `fireballwand.cheatsword` |
| `/cheatmace` | Get the Cheat Mace | `fireballwand.cheatmace` |
| `/diamond_armorcheat` | Get the Diamond Cheat Armor | `fireballwand.diamond_armorcheat` |
| `/netherite_armorcheat` | Get the Netherite Cheat Armor | `fireballwand.netherite_armorcheat` |

### Plugin Management

| Command | Description | Permission |
|:---|:---|:---|
| `/fw reload` | Reload every configuration file | `fireballwand.admin` |
| `/tpareload` | Reload the teleportation request settings | `fireballwand.tpa.admin` |

### Additional permissions

| Permission | Description |
|:---|:---|
| `fireballwand.staff.teleport` | Gives the teleport compass in Staff Mode |
| `fireballwand.staff.invsee` | View a player's inventory in Staff Mode |
| `fireballwand.staff.invsee.edit` | Edit a player's inventory through the viewer |
| `fireballwand.vanish.level.1` to `.4` | Vanish levels (a higher level sees the lower ones) |
| `fireballwand.home.bypass.warmup` / `.cooldown` | Skip the home delay / cooldown |
| `fireballwand.spawn.bypass.warmup` / `.cooldown` | Skip the spawn delay / cooldown |
| `fireballwand.negativesnowball.bypass` | Immunity to the Cursed Snowball |
| `fireballwand.swordup.bypass` | Immunity to the Sword Up |
| `fireballwand.chat.color` / `.chat.color.hex` | Use `&` colors / hex colors in chat |
| `fireballwand.money.use` | View balances (default: true) |
| `fireballwand.money.pay` | Pay another player (default: true) |
| `fireballwand.money.top` | View the economy leaderboard (default: true) |
| `fireballwand.money.admin` | Manage balances and reload `Money.yml` (default: op) |

---

## 🧩 Compatibility

FireBallWand is developed for Paper and Purpur servers.

Compatibility can depend on the Minecraft version and individual API changes. Check the release notes of each version before updating your production server.

---

## 🐛 Bug Reports & Suggestions

Found a bug or have an idea for a new feature?

Please report it through the project's issue tracker.

When reporting an issue, include:

- Minecraft version.
- Server software and version.
- FireBallWand version.
- Relevant console logs.
- Steps to reproduce the issue.

Clear reports make troubleshooting easier.

---

## 📄 License

**All Rights Reserved © Zyumie**

All rights reserved. Unauthorized copying, modification, redistribution or use of this software is prohibited without prior permission from the author.

---

<div align="center">

**Developed with ❤️ by Zyumie**

🔥 FireBallWand — Administration, Moderation & Custom Gameplay.

</div>
