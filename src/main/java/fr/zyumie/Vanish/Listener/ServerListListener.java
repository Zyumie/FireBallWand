package fr.zyumie.Vanish.Listener;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import fr.zyumie.fireballwand.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ServerListListener implements Listener {

    private final Main plugin;

    public ServerListListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Cet event est déclenché sur un thread réseau (async) :
     * on travaille uniquement avec des UUID et des structures thread-safe,
     * sans appeler Bukkit.getPlayer().
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPing(PaperServerListPingEvent event) {
        VanishManager vm = plugin.getVanishManager();

        // 1. Compteur de joueurs sans les vanishés
        event.setNumPlayers(vm.getVisiblePlayerCount());

        // 2. Liste affichée au survol du compteur
        event.getListedPlayers().removeIf(info -> vm.isVanished(info.id()));
    }
}
