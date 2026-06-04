package fr.zyumie.Vanish;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import fr.zyumie.fireballwand.Main;

public class ServerListListener implements Listener {

    private final Main plugin;

    public ServerListListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        int visible = plugin.getVanishManager().getVisiblePlayerCount();
        event.getNumPlayers();
    }
}