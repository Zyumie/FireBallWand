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

  Armure qui repousse / kick automatiquement les joueurs autour du porteur
  *(rayon configurable — les OP ne sont pas affectés)*.

- **❄️ FreezeWand**

  Épée qui freeze les joueurs en **mode Survie** *(messages configurables)*.

- **⚔️ Cheat Sword**

  Épée surpuissante avec enchantements et attributs abusés.

- **🔨 Cheat Mace**

  Mace surpuissante avec enchantements abusés.

- **💎 Cheat Armor**

  Sets complets **diamant et netherite** :
  - enchantements extrêmes
  - attributs custom
  - unbreakable

- **👻 Vanish Hiérarchique**

  Système de vanish avancé avec gestion des grades :
  - invisible aux joueurs normaux
  - compatible LuckPerms
  - possibilité de vanisher un autre joueur (`/vanish <joueur>`)
  - les staffs supérieurs voient les vanishés des grades inférieurs
  - les staffs inférieurs ne voient pas les vanishés des grades supérieurs
  - **action bar** permanente indiquant l'état vanish
  - **double-sneak** pour basculer en mode spectateur (et retour)
  - messages de join/quit supprimés automatiquement

- **💬 Messages Privés Stylés**

  Système de messagerie privée avancé :
  - formatage sur mesure (`(To joueur)` / `(From joueur)`)
  - prise en charge des couleurs classiques `&` et HEX `&#RRGGBB`
  - intégration des glyphes et emojis **ItemsAdder**
  - commande de réponse rapide (`/r` ou `/reply`)

---

### 📚 Système Admin & Chat

- Message de bienvenue (configurable, affiché à la première connexion)

- Format du chat global et des messages privés entièrement personnalisable

- Couleurs dans le chat et les MPs par permission (`&codes` et `&#HEX`)

- Compatibilité **LuckPerms** (préfixes, suffixes)

- Compatibilité **ItemsAdder** (glyphes/images dans le chat et les MPs — soft-depend)

- Système de vanish hiérarchique par niveau

---

### 🔧 Installation

1. Installer un serveur compatible **PaperMC ou Purpur** (1.21+ recommandé).
2. Télécharger `FireBallWand.jar` et le placer dans le dossier `plugins/`.
3. Redémarrer le serveur.
4. *(Optionnel)* Installer **LuckPerms** et/ou **ItemsAdder** pour les fonctionnalités avancées de chat.

---

### 🎮 Commandes

| Commande | Description | Permission |
|---|---|---|
| `/firewand` | Donne la FireWand | `fireballwand.firewand` |
| `/kickarmor` | Donne la KickArmor | `fireballwand.kickarmor` |
| `/freezewand` | Donne la FreezeWand | `fireballwand.freezewand` |
| `/unfreezewand-all` | Dégèle tous les joueurs | `fireballwand.unfreezewand` |
| `/cheatsword` | Donne l'épée cheat | `fireballwand.cheatsword` |
| `/cheatmace` | Donne la mace cheat | `fireballwand.cheatmace` |
| `/diamond_armorcheat` | Donne l'armure cheat diamant | `fireballwand.diamond_armorcheat` |
| `/netherite_armorcheat` | Donne l'armure cheat netherite | `fireballwand.netherite_armorcheat` |
| `/vanish` | Active/Désactive son propre vanish | `fireballwand.vanish.level.1` |
| `/vanish <joueur>` | Active/Désactive le vanish d'un autre joueur | `fireballwand.vanish.others` |
| `/msg <joueur> <msg>` | Envoie un message privé à un joueur | *Aucune (Disponible pour tous)* |
| `/r <msg>` *(ou `/reply`)* | Répond au dernier interlocuteur | *Aucune (Disponible pour tous)* |

---

### 🔑 Permissions

#### Chat & Messages Privés

| Permission | Description | Défaut |
|---|---|---|
| `fireballwand.chat.color` | Codes couleur `&x` dans le chat et les MPs | `false` |
| `fireballwand.chat.color.hex` | Codes couleur `&#RRGGBB` dans le chat et les MPs | `false` |

#### Vanish

| Permission | Description | Défaut |
|---|---|---|
| `fireballwand.vanish.others` | `/vanish <joueur>` | op |
| `fireballwand.vanish.level.1` | Niveau Helper — vanish basique | `false` |
| `fireballwand.vanish.level.2` | Niveau Modérateur — voit les level 1 vanishés | `false` |
| `fireballwand.vanish.level.3` | Niveau Administrateur — voit les level 1 et 2 vanishés | `false` |
| `fireballwand.vanish.level.4` | Niveau Propriétaire — voit tous les vanishés | op |

> Les niveaux sont **hiérarchiques** : attribuer `level.3` dans LuckPerms donne automatiquement `level.2` et `level.1`.

---

### ⚙️ Configuration

Tous les éléments du plugin sont personnalisables via `config.yml` :

| Section | Contenu |
|---|---|
| `welcome` | Message de bienvenue (activer/désactiver, texte) |
| `chat` | Format, couleurs par permission, ItemsAdder, messages vanish-chat |
| `messages` | Formats des MPs (`format-to`, `format-from`) |
| `vanish` | Niveaux, action bar, double-sneak spectateur, tous les messages |
| `firewand` | Nom de l'item, puissance, incendie |
| `freezewand` | Nom de l'item, tous les messages |
| `kickarmor` | Nom de l'item, rayon de kick, message de kick |
| `cheats` | Noms des items (épée, mace, armure) |

---

### ⚠️ Notes

- Plugin prévu pour **serveurs fun, events ou SMP privés**
- Non recommandé pour serveurs publics compétitifs
- Usage des items réservé aux **OP/Admins**
- LuckPerms et ItemsAdder sont des **soft-depends** (optionnels)

---

© 2026 Zyumie. Tous droits réservés.
Aucune utilisation, modification ou redistribution sans autorisation explicite.
