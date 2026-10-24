<div align="center">

[![Paper](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/paper_vector.svg)](https://papermc.io/)
[![Purpur](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/purpur_vector.svg)](https://purpurmc.org/)

</div>

---

## 🔥 FireBallWand — Plugin Minecraft

### 📌 Version 3.5.0

- Plugin : **FireBallWand**
- Auteur : Zyumie (aka AyanoBrz)
- Spécialité : Donne accès à des **items fun et cheat** destinés aux OP/Admins pour interagir avec les joueurs.

---

### ✨ Fonctionnalités

- **🔥 FireWand**
  Bâton tirant des fireballs à forte puissance *(puissance et incendie configurables)*.

- **👢 KickArmor**
  Armure qui repousse / kick automatiquement les joueurs autour du porteur *(rayon configurable — les OP ne sont pas affectés)*.

- **❄️ FreezeWand**
  Épée qui freeze les joueurs en **mode Survie** *(messages configurables)*.

- **☠️ Potion of Death**
  Splash Potion personnalisée qui tue instantanément la cible (même en Créatif / Aventure) tout en **conservant son inventaire complet (Keep Inventory)**. *(Supporte le bypass permission/OP, l'annonce dans le chat activable/désactivable et les messages configurables)*.

- **💨 WinCharge Boosted**
  Charge de vent personnalisée qui génère une explosion mortelle pour les joueurs touchés avec **Keep Inventory**.

- **⚔️ Cheat Sword**
  Épée surpuissante avec enchantements et attributs abusés.

- **🔨 Cheat Mace**
  Mace surpuissante avec enchantements abusés.

- **💎 Cheat Armor**
  Sets complets **diamant et netherite** (enchantements extrêmes, attributs custom, unbreakable).

- **👻 Vanish Hiérarchique**
  Système de vanish avancé avec gestion des grades (invisible aux joueurs normaux, compatible LuckPerms, action bar, double-sneak spectateur).

- **💬 Messages Privés Stylés**
  Système de messagerie privée avancé (`/msg`, `/reply`, `/r`, `/re`) avec support HEX, couleurs `&` et emojis ItemsAdder.

---

### 📚 Système Admin & Chat

- Message de bienvenue (configurable, affiché à la première connexion).
- Format du chat global et des messages privés entièrement personnalisable.
- Couleurs dans le chat et les MPs par permission (`&codes` et `&#HEX`).
- Compatibilité **LuckPerms** (préfixes, suffixes).
- Compatibilité **ItemsAdder** (glyphes/images dans le chat et les MPs — soft-depend).
- Système de vanish hiérarchique par niveau.

---

### 🔧 Installation

1. Installer un serveur compatible **PaperMC ou Purpur** (1.21+ recommandé).
2. Télécharger `FireBallWand.jar` et le placer dans le dossier `plugins/`.
3. Redémarrer le serveur.
4. *(Optionnel)* Installer **LuckPerms** et/ou **ItemsAdder** pour les fonctionnalités avancées.

---

### 🎮 Commandes

| Commande | Description | Permission |
|---|---|---|
| `/firewand` | Donne la FireWand | `fireballwand.firewand` |
| `/kickarmor` | Donne la KickArmor | `fireballwand.kickarmor` |
| `/freezewand` | Donne la FreezeWand | `fireballwand.freezewand` |
| `/unfreezewand-all` | Dégèle tous les joueurs | `fireballwand.unfreezewand` |
| `/potionofdeath` *(alias `/pod`)* | Donne la Potion of Death | `fireballwand.potion-of-death` |
| `/wincharge_boosted` | Donne la WinCharge Boosted | `fireballwand.wincharge_boosted` |
| `/cheatsword` | Donne l'épée cheat | `fireballwand.cheatsword` |
| `/cheatmace` | Donne la mace cheat | `fireballwand.cheatmace` |
| `/diamond_armorcheat` | Donne l'armure cheat diamant | `fireballwand.diamond_armorcheat` |
| `/netherite_armorcheat` | Donne l'armure cheat netherite | `fireballwand.netherite_armorcheat` |
| `/vanish [joueur]` | Active/Désactive le vanish | `fireballwand.vanish.level.1` |
| `/msg <joueur> <msg>` | Envoie un message privé à un joueur | *Aucune (Disponible pour tous)* |
| `/reply <msg>` *(alias `/r`, `/re`)* | Répond au dernier interlocuteur | *Aucune (Disponible pour tous)* |

---

### 🔑 Permissions

#### Items & Commandes Admin

| Permission | Description | Défaut |
|---|---|---|
| `fireballwand.potion-of-death` | Give et utilisation de la Potion of Death | op |
| `fireballwand.potion-of-death.bypass` | Immunité contre la Potion of Death | op |
| `fireballwand.wincharge_boosted` | Give et utilisation de la WinCharge Boosted | op |
| `fireballwand.wincharge.bypass` | Immunité contre les explosions de la Wincharge | op |
| `fireballwand.firewand` | Give la FireWand | op |
| `fireballwand.kickarmor` | Give la KickArmor | op |
| `fireballwand.freezewand` | Give la FreezeWand | op |
| `fireballwand.unfreezewand` | Dégeler tous les joueurs | op |
| `fireballwand.cheatsword` | Give l'épée Cheat | op |
| `fireballwand.cheatmace` | Give la Mace Cheat | op |
| `fireballwand.diamond_armorcheat` | Give l'armure Diamant Cheat | op |
| `fireballwand.netherite_armorcheat` | Give l'armure Netherite Cheat | op |

#### Chat & Messages Privés

| Permission | Description | Défaut |
|---|---|---|
| `fireballwand.msg` | Envoyer des messages privés | true |
| `fireballwand.reply` | Répondre aux messages privés | true |
| `fireballwand.chat.color` | Codes couleur `&x` dans le chat et MPs | false |
| `fireballwand.chat.color.hex` | Codes couleur HEX `&#RRGGBB` dans le chat | false |

#### Vanish

| Permission | Description | Défaut |
|---|---|---|
| `fireballwand.vanish.others` | Vanisher un autre joueur | op |
| `fireballwand.vanish.level.1` | Niveau Helper — vanish basique | false |
| `fireballwand.vanish.level.2` | Niveau Modérateur — voit les level 1 | false |
| `fireballwand.vanish.level.3` | Niveau Admin — voit les level 1 et 2 | false |
| `fireballwand.vanish.level.4` | Niveau Propriétaire — voit tous les vanishés | op |

---

### ⚙️ Configuration (`config.yml`)

Tous les éléments du plugin sont personnalisables via `config.yml` :

| Section | Contenu |
|---|---|
| `potion-of-death` | Nom, lore, activation/désactivation de l'annonce dans le chat et tous les messages. |
| `wincharge` | Nom, lore, cooldown, rayon, particules et messages. |
| `welcome` | Message de bienvenue (activer/désactiver, texte). |
| `chat` | Format, couleurs par permission, ItemsAdder, messages vanish-chat. |
| `messages` | Formats des MPs (`format-to`, `format-from`). |
| `vanish` | Niveaux, action bar, double-sneak spectateur, tous les messages. |
| `firewand` / `freezewand` / `kickarmor` | Propriétés des items et messages associatifs. |

---

### ⚠️ Notes

- Plugin prévu pour **serveurs fun, events ou SMP privés**.
- Non recommandé pour serveurs publics compétitifs.
- Usage des items réservé aux **OP/Admins**.
- LuckPerms et ItemsAdder sont des **soft-depends** (optionnels).

---

© 2026 Zyumie. Tous droits réservés.
Aucune utilisation, modification ou redistribution sans autorisation explicite.
