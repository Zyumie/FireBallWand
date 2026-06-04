package fr.zyumie.Vanish;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import fr.zyumie.fireballwand.Main;

public class VanishListener implements Listener {

    private final Main plugin;

    public VanishListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Quand un joueur rejoint : lui masquer les joueurs vanishés qu'il
     * ne doit pas voir, et si lui-même était vanishé (reconnexion),
     * le re-masquer aux autres.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();
        VanishManager vm = plugin.getVanishManager();

        // Appliquer les masques selon la hiérarchie
        vm.applyVanishOnJoin(joining);

        // Si ce joueur était déjà dans la liste vanish (reconnexion improbable
        // mais possible avec des plugins de sauvegarde d'état), on le re-cache.
        if (vm.isVanished(joining)) {
            vm.enableVanish(joining);
            // Supprimer le message de connexion
            event.setJoinMessage(null);
        }
    }

    /**
     * Quand un joueur vanishé quitte : supprimer le message de déconnexion.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player leaving = event.getPlayer();
        VanishManager vm = plugin.getVanishManager();

        if (vm.isVanished(leaving)) {
            event.setQuitMessage(null);
            // On retire de la map proprement
            vm.disableVanish(leaving);
        }
    }
}