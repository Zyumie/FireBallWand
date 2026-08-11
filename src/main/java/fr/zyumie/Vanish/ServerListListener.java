package fr.zyumie.Vanish;

import fr.zyumie.fireballwand.Main;
import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ServerListListener implements Listener {

    private final Main plugin;

    public ServerListListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPing(PaperServerListPingEvent event) {

        int visiblePlayers = plugin.getVanishManager().getVisiblePlayerCount();

        // Évite d'avoir un nombre négatif
        if (visiblePlayers < 0) {
            visiblePlayers = 0;
        }

        event.setNumPlayers(visiblePlayers);
    }
}