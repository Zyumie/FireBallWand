

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

**FireBallWand** is a modular Minecraft plugin designed to bring administration tools, moderation features and custom items together in one versatile solution.

Whether you're managing a Survival server, a private SMP or an event-based community, FireBallWand provides the tools needed to manage your players, moderate discreetly and customize your gameplay experience.

From an advanced **Vanish system** and **Staff Mode** to powerful custom weapons, teleportation requests and private messaging, every feature is designed to be configurable and easy to use.

### ✨ Main Highlights

- 🛡️ Complete Staff Mode and moderation toolkit.
- 👻 Advanced hierarchical Vanish system.
- ⭐ God Mode with configurable behavior.
- 💥 Custom weapons, armor and special items.
- 💬 Chat formatting and private messaging.
- 🧭 Interactive teleportation request system.
- 🎒 Automatic and configurable Starter Kit.
- 📂 Separate YAML configuration files for each module.
- 🔌 Optional integrations with popular plugins.

---

## 🛡️ Features

### Staff Mode — `/staff`

A dedicated moderation toolkit that allows staff members to moderate players without switching to Creative Mode.

**How it works:**

- Toggle Staff Mode with `/staff`.
- Automatically receive your authorized moderation items.
- Execute `/staff` again to leave Staff Mode.
- Each item is only provided when the corresponding permission is available.

**Available tools:**

| Item | Function |
|:---|:---|
| 👻 Vanish Item | Toggle your Vanish status |
| ❄️ FreezeWand | Freeze or unfreeze targeted players |
| ⭐ God Mode Item | Toggle God Mode |
| 🧭 Teleport Compass | Teleport to the next online player |

**Inventory protection:**

- Items are placed in free inventory slots only.
- Existing inventory items are never replaced.
- Staff items cannot be dropped or stored in chests.
- Moving protected items to the offhand is blocked.
- Staff items are removed when disconnecting.
- Item names, materials, slots and messages are configurable.

The entire module can be disabled through `Staff.yml`.

### ⭐ God Mode

**Commands:** `/godmod`, `/ungodmod`

An invulnerability system for administrators and staff members.

- Cancels incoming damage and hunger depletion while active.
- The void and `/kill` can still affect players.
- Can also be activated through the Staff Mode item.
- Includes short aliases: `/gm` and `/ugm`.

### 👻 Advanced Hierarchical Vanish

An advanced Vanish system designed for discreet staff moderation.

**Rank hierarchy:**

- Higher staff ranks can see lower-ranked vanished players.
- Lower staff ranks cannot see higher-ranked vanished players.
- Visibility is managed through permission-based levels.

**Persistence and discretion:**

- Vanish status is preserved after reconnecting.
- Join and quit messages are hidden while vanished.
- Optional fake join and quit messages when toggling Vanish.
- Persistent ActionBar reminder.
- Vanished players are excluded from the server-list player count and hover player list.
- Vanished players cannot pick up items.
- TAB integration hides vanished players from unauthorized viewers.

**Additional features:**

- Double-sneak to switch to Spectator Mode and return to the previous game mode.
- `/vanish list` displays currently vanished players.
- Chat protection prevents messages from being sent unless they end with `/`.

All Vanish settings, permission levels and messages can be customized in `Vanish.yml`.

### 💬 Chat & Private Messages

Customize the way your community communicates.

**Chat formatting:**

- Permission-based color access.
- Standard Minecraft color codes using `&`.
- Hexadecimal colors using `&#RRGGBB`.
- Rank prefixes and suffixes through LuckPerms.
- ItemsAdder glyph support for custom rank icons.
- PlaceholderAPI support in chat formats and welcome messages.

**Private messaging:**

- `/msg` — Send a private message.
- `/reply` — Reply to the last private conversation.

**Welcome system:**

- Configurable first-join welcome message.
- PlaceholderAPI support.

### 🧭 Teleportation Request System

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

### 🎒 Starter Kit

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

### 💥 Custom Items & Admin Arsenal

FireBallWand also includes a collection of powerful custom items designed for administration, testing and fun events.

| Item | Description |
|:---|:---|
| 🔥 **FireWand** | Launches explosive fireballs. |
| 👢 **KickArmor** | Automatically knocks back nearby players who get too close. |
| ❄️ **FreezeWand** | Freezes targeted players in place. |
| ⚡ **WinCharge Boosted** | A powerful explosive charge with x3 boosted power. |
| ☠️ **Potion of Death** | A deadly splash potion that bypasses Creative Mode and handles Keep Inventory. |
| ⚔️ **Cheat Sword** | An overpowered custom sword. |
| 🔨 **Cheat Mace** | An enhanced custom mace. |
| 💎 **Diamond Cheat Armor** | A complete diamond armor set with extreme attributes and enchantments. |
| 🛡️ **Netherite Cheat Armor** | A complete netherite armor set with extreme attributes and enchantments. |

Certain equipment includes level 255+ enchantments and custom attributes.

All custom item settings are managed through `CustomCheat.yml`.

---

## ⚙️ Configuration System

FireBallWand uses a modular YAML configuration architecture.

Each feature has its own dedicated configuration file, making customization and maintenance easier.

| Configuration file | Purpose |
|:---|:---|
| `config.yml` | General settings, chat and private messages |
| `Staff.yml` | Staff Mode, moderation items and God Mode |
| `Vanish.yml` | Vanish hierarchy, visibility and messages |
| `CustomCheat.yml` | Custom weapons, armor and special items |
| `StartKit.yml` | Starter Kit contents and cooldowns |
| `Tpa.yml` | Teleportation requests and protection settings |

**Reload configuration:**

```text
/fw reload
```

Permission: `fireballwand.admin`

---

## 🔌 Optional Integrations

FireBallWand works without additional plugins.

The following integrations can unlock additional functionality:

| Plugin | Integration |
|:---|:---|
| [LuckPerms](https://luckperms.net/) | Rank prefixes, suffixes and permissions |
| [ItemsAdder](https://itemsadder.com/) | Custom glyphs in chat |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Placeholder support |
| [TAB](https://modrinth.com/plugin/tab-was-taken) | Vanish compatibility with the tab list |

All integrations are optional.

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

**Optional:** Install LuckPerms, ItemsAdder, PlaceholderAPI and/or TAB to enable their respective integrations.

> **Using TAB?** Assign the appropriate `tab.seevanished` permission to staff members who should be able to see vanished players in the tab list.

---

## 📋 Commands & Permissions

### Administration & Moderation

| Command | Description | Permission |
|:---|:---|:---|
| `/staff` | Toggle Staff Mode | `fireballwand.staff` |
| `/godmod` / `/gm` | Enable God Mode | `fireballwand.god` |
| `/ungodmod` / `/ugm` | Disable God Mode | `fireballwand.god` |
| `/vanish` / `/fv` | Toggle your Vanish status | `fireballwand.vanish.level.1` |
| `/vanish <player>` | Toggle another player's Vanish | `fireballwand.vanish.others` |
| `/vanish list` | Display vanished players | `fireballwand.vanish.list` |

### Utility & Communication

| Command | Description | Permission |
|:---|:---|:---|
| `/starterkit` | Claim the Starter Kit | `fireballwand.starterkit` |
| `/tpa <player>` | Request teleportation | `fireballwand.tpa` |
| `/tpahere <player>` | Invite a player to teleport to you | `fireballwand.tpahere` |
| `/tpaccept` | Accept a request | Verify `Tpa.yml` |
| `/tpdeny` | Deny a request | Verify `Tpa.yml` |
| `/tpacancel` | Cancel a request | Verify `Tpa.yml` |
| `/msg <player>` | Send a private message | `fireballwand.msg` |
| `/reply` | Reply to a private message | `fireballwand.reply` |

### Custom Items

| Command | Description | Permission |
|:---|:---|:---|
| `/firewand` | Get the FireWand | `fireballwand.firewand` |
| `/kickarmor` | Get the KickArmor | `fireballwand.kickarmor` |
| `/freezewand` | Get the FreezeWand | `fireballwand.freezewand` |
| `/wincharge_boosted` | Get the boosted WinCharge | `fireballwand.wincharge_boosted` |
| `/potionofdeath` / `/pod` | Get the Potion of Death | `fireballwand.potion-of-death` |
| `/cheatsword` | Get the Cheat Sword | `fireballwand.cheatsword` |
| `/cheatmace` | Get the Cheat Mace | `fireballwand.cheatmace` |
| `/diamond_armorcheat` | Get the Diamond Cheat Armor | `fireballwand.diamond_armorcheat` |
| `/netherite_armorcheat` | Get the Netherite Cheat Armor | `fireballwand.netherite_armorcheat` |

### Plugin Management

| Command | Description | Permission |
|:---|:---|:---|
| `/fw reload` | Reload plugin configurations | `fireballwand.admin` |

> **Note:** Teleportation permission nodes should be checked against the actual command registration and configuration before assigning them to ranks.

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

