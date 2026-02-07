package fr.zyumie.Listener;

import fr.zyumie.fireballwand.Main;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class ChatManager implements Listener {
   
    private final Main plugin;
    
    public ChatManager(Main plugin) {
        this.plugin = plugin;
    }
	
    // Format du Chat
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
    	
    	
    	// Vérifie le Préfix & Suffix du Joueur avec LuckPerm
        Player player = event.getPlayer();

        String prefix = "";
        String suffix = "";
        

        if (plugin.getLuckPermsHook() != null) {
            prefix = plugin.getLuckPermsHook().getPrefix(event.getPlayer());
        }
        
        
        // Met le format du Chat
        event.setFormat(
            ChatColor.translateAlternateColorCodes('&',
                prefix + player.getName() + suffix + " : " + "%2$s"
            )
        );
    }

    
    // Message de Bienvenue
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        // Vérifie si c’est la première fois
        if (event.getPlayer().hasPlayedBefore()) return;

        if (!plugin.getConfig().getBoolean("welcome-message")) return;

        String msg = plugin.getConfig().getString("welcome.msg");
        if (msg == null) return;

        msg = msg.replace("%player%", event.getPlayer().getName());
        msg = ChatColor.translateAlternateColorCodes('&', msg);

        event.getPlayer().sendMessage(msg);
    }
        
}
