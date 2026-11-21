package fr.zyumie.Money.Listener;

import fr.zyumie.Money.MoneyModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final MoneyModule module;

    public PlayerListener(MoneyModule module) { this.module = module; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!module.isEnabled()) return;
        // Init du solde de départ pour un nouveau joueur (lecture déjà faite en RAM).
        module.service().touch(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (!module.isEnabled()) return;
        module.service().flush();
    }
}