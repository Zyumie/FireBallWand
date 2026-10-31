<div align="center">

[![Paper](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/paper_vector.svg)](https://papermc.io/)
[![Purpur](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/purpur_vector.svg)](https://purpurmc.org/)

</div>

---

# 🔥 FireBallWand — The Ultimate Admin & Fun Tool!

Looking to boost your server, provide seamless invisible moderation, and equip your administrators with an extraordinary arsenal? **FireBallWand** is the all-in-one plugin you need. Designed to combine **maximum performance** with **pure fun**, it transforms the gameplay experience on private SMP, fun, and event servers.

---

## 🌟 Why Choose FireBallWand?

* **⚡ Zero-Lag Performance:** Asynchronous saves and highly optimized code to guarantee a rock-solid 20 TPS no matter what happens.
* **📂 Modular & Clean:** No more overloaded config files! The plugin separates its features into dedicated YAML files (`Vanish.yml`, `CustomCheat.yml`, `StartKit.yml`, `Tpa.yml`).
* **🔮 Modern Player Experience:** Interactive chat interfaces, clickable buttons, and seamless navigation.

---

## 🚀 Key Features

### 🔮 High-Performance TPA System
* **Unified Commands:** `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpacancel`.
* **Interactive Chat:** Accept or deny teleport requests with a single click directly in the chat!
* **Safety & Warmup:** Configurable delay before teleportation (cancelled on movement or damage) paired with post-teleport invincibility protection displayed in the ActionBar.
* **Asynchronous File I/O:** No server stutter or TPS drops when saving cooldown data.

### 👻 Intelligent Hierarchical Vanish
* **Level-Based Hierarchy:** Higher staff ranks can see lower vanished staff, but lower ranks cannot see higher ranks.
* **Instant Spectator Mode:** A quick *double-sneak* allows staff to instantly switch to spectator mode (and back).
* **Absolute Discretion:** Join/leave messages are hidden, a persistent action bar reminder is displayed, and the server list player count (`ServerListPing`) is smartly synchronized.

### 🎒 Ready-to-Use Starter Kit (`/starterkit`)
* **Balanced Adventurer Kit:** Enchanted iron armor, diamond tools, food, and survival consumables.
* **Automatic Granting:** Automatically given to new players upon their very first join.
* **Managed Cooldowns:** Claim delay management with dedicated *bypass* permissions.

### 💥 Extraordinary Arsenal for Admins
* **🔥 FireWand:** A devastating rod that launches explosive fireballs.
* **👢 KickArmor:** Protective chestplate that automatically pushes/kicks curious players getting too close.
* **❄️ FreezeWand:** Instantly freezes target players in place while in Survival mode.
* **⚡ WinCharge Boosted (x3):** A high-powered explosive charge capable of instantly eliminating targets.
* **☠️ Potion of Death:** The famous splash potion for instant elimination (ignores Creative mode and manages Keep Inventory).
* **⚔️ Cheat Weapons & Armors:** Overpowered swords, maces, and complete Diamond/Netherite sets with extreme attributes and enchantments (Level 255+).

---

## ⚙️ Modular Architecture (YAML)

Keep total control over your configuration thanks to a clean file layout:
* 📁 `config.yml`: General settings, chat, and private messages.
* 📁 `CustomCheat.yml`: Full customization of all special items, weapons, and armors.
* 📁 `Vanish.yml`: Management of vanish levels and associated messages.
* 📁 `StartKit.yml`: Complete configuration for the starter kit.
* 📁 `Tpa.yml`: Complete settings for the teleportation system.

---

## 📥 Installation

1. Make sure you are running a server on **PaperMC** or **Purpur** (1.21+ recommended).
2. Drag `FireBallWand.jar` into your `plugins/` folder.
3. Restart the server: configuration files will generate automatically.
4. *(Optional)* Install **LuckPerms** and/or **ItemsAdder** to enjoy advanced chat and rank hooks!

---

## 🎮 Quick Command List

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/starterkit` | Claim the starter kit | `fireballwand.starterkit` |
| `/tpa <player>` | Request to teleport to a player | `fireballwand.tpa` |
| `/tpahere <player>` | Invite a player to teleport to you | `fireballwand.tpahere` |
| `/vanish` | Toggle your vanish status | `fireballwand.vanish.level.1` |
| `/firewand` | Get the FireWand | `fireballwand.firewand` |
| `/kickarmor` | Get the KickArmor | `fireballwand.kickarmor` |
| `/freezewand` | Get the FreezeWand | `fireballwand.freezewand` |
| `/wincharge_boosted` | Get the boosted WinCharge | `fireballwand.wincharge_boosted` |
| `/potionofdeath` (`/pod`) | Get the Potion of Death | `fireballwand.potion-of-death` |
| `/cheatsword` / `/cheatmace` | Get the cheat weapons | `fireballwand.cheatsword` / `.cheatmace` |
| `/diamond_armorcheat` | Get the diamond cheat armor | `fireballwand.diamond_armorcheat` |
| `/netherite_armorcheat` | Get the netherite cheat armor | `fireballwand.netherite_armorcheat` |

---

<div align="center">

© 2026 Zyumie. Tous droits réservés.  
Aucune utilisation, modification ou redistribution sans autorisation explicite.

</div>
