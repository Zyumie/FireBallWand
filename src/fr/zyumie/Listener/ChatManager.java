package fr.zyumie.Listener;

import fr.zyumie.fireballwand.Main;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.cacheddata.CachedMetaData;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class ChatManager implements Listener {
   
    private final Main plugin;
    private final LuckPerms luckPerms;
    
    public ChatManager(Main plugin) {
        this.plugin = plugin;
        this.luckPerms = LuckPermsProvider.get();
    }
	
    // Format du Chat
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
    	
    	
    	// Vérifie le Préfix & Suffix du Joueur avec LuckPerm
        Player player = event.getPlayer();

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return;

        CachedMetaData meta = user.getCachedData().getMetaData();

        String prefix = meta.getPrefix();
        String suffix = meta.getSuffix();
        
        if (prefix == null) prefix = "";
        if (suffix == null) suffix = "";

        
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
