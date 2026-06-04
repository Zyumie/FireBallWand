package fr.zyumie.Vanish;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import fr.zyumie.fireballwand.Main;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gère l'état du vanish et la visibilité hiérarchique.
 *
 * Logique de niveau :
 *   Permission  "Prototype.vanish.level.N"  →  niveau N (entier ≥ 1).
 *   Un joueur de niveau N voit tous les joueurs vanishés dont le niveau ≤ N.
 *   Autrement dit : un grade supérieur voit les grades inférieurs (et lui-même),
 *   mais pas les grades strictement supérieurs.
 *
 *   Exemple :
 *     Helper  = level 1  → ne voit personne en vanish
 *     Modo    = level 2  → voit les Helper vanishés
 *     Admin   = level 3  → voit les Modo et Helper vanishés
 *     (chacun se voit lui-même puisqu'il est l'auteur du toggle)
 */
public class VanishManager {

    private final Main plugin;

    /** UUID → niveau au moment où le vanish a été activé */
    private final Map<UUID, Integer> vanishedPlayers = new HashMap<>();

    /** Niveau maximum autorisé (lu depuis config.yml, défaut 10) */
    private final int maxLevel;

    public VanishManager(Main plugin) {
        this.plugin = plugin;
        this.maxLevel = plugin.getConfig().getInt("vanish.max-level", 10);
    }

    // ---------------------------------------------------------------
    //  Résolution du niveau
    // ---------------------------------------------------------------

    /**
     * Retourne le niveau de vanish du joueur.
     * On cherche la permission "fireballwand.vanish.level.N" la plus haute
     * que le joueur possède, de maxLevel à 1.
     * Retourne 0 si aucune permission de niveau n'est trouvée.
     */
    public int getVanishLevel(Player player) {
        for (int lvl = maxLevel; lvl >= 1; lvl--) {
            if (player.hasPermission("fireballwand.vanish.level." + lvl)) {
                return lvl;
            }
        }
        return 0; // pas de permission
    }

    /**
     * Raccourci : le joueur peut-il utiliser le vanish ?
     * (niveau ≥ 1 OU permission générique "fireballwand.vanish")
     */
    public boolean canVanish(Player player) {
        return getVanishLevel(player) >= 1
                || player.hasPermission("fireballwand.vanish");
    }

    // ---------------------------------------------------------------
    //  Toggle vanish
    // ---------------------------------------------------------------

    public boolean isVanished(Player player) {
        return vanishedPlayers.containsKey(player.getUniqueId());
    }

    /**
     * Active le vanish pour le joueur et met à jour la visibilité
     * de tous les joueurs en ligne.
     */
    public void enableVanish(Player player) {
        int level = Math.max(getVanishLevel(player), 1);
        vanishedPlayers.put(player.getUniqueId(), level);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            // online peut-il voir "player" vanishé ?
            if (!canSeeVanished(online, level)) {
                online.hidePlayer(plugin, player);
            }
        }
    }

    /**
     * Désactive le vanish et rend le joueur visible à tous.
     */
    public void disableVanish(Player player) {
        vanishedPlayers.remove(player.getUniqueId());
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }
    }

    // ---------------------------------------------------------------
    //  Rejoindre le serveur
    // ---------------------------------------------------------------

    /**
     * Appelé à chaque connexion : applique les masques nécessaires
     * pour le joueur qui arrive.
     */
    public void applyVanishOnJoin(Player joining) {
        for (Map.Entry<UUID, Integer> entry : vanishedPlayers.entrySet()) {
            Player vanished = Bukkit.getPlayer(entry.getKey());
            if (vanished == null || vanished.equals(joining)) continue;

            if (!canSeeVanished(joining, entry.getValue())) {
                joining.hidePlayer(plugin, vanished);
            }
        }
    }

    // ---------------------------------------------------------------
    //  Visibilité hiérarchique
    // ---------------------------------------------------------------

    /**
     * Détermine si "observer" peut voir un joueur vanishé de "vanishedLevel".
     * Règle : observer.level >= vanishedLevel  →  visible.
     */
    public boolean canSeeVanished(Player observer, int vanishedLevel) {
        int observerLevel = getVanishLevel(observer);
        return observerLevel >= vanishedLevel;
    }

    // ---------------------------------------------------------------
    //  Utilitaires
    // ---------------------------------------------------------------

    /** Nombre de joueurs réellement visibles (pour le ServerListPing). */
    public int getVisiblePlayerCount() {
        return Math.max(0, Bukkit.getOnlinePlayers().size() - vanishedPlayers.size());
    }

    /** Vue non-modifiable pour usage externe (ServerListListener, etc.). */
    public Set<UUID> getVanishedUUIDs() {
        return Collections.unmodifiableSet(vanishedPlayers.keySet());
    }

    /** Nettoyage à l'arrêt du plugin. */
    public void clear() {
        vanishedPlayers.clear();
    }
}