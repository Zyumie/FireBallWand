package fr.zyumie.Listener;

import fr.zyumie.fireballwand.Main;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    

    // Pattern HEX (&?#RRGGBB)
    private static final Pattern HEX_PATTERN = Pattern.compile("(&)?#([a-fA-F0-9]{6})");

    // Traduction & + HEX
    public static String colorize(String text) {
        if (text == null) return "";

        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(
                    buffer,
                    ChatColor.of("#" + matcher.group(2)).toString()
            );
        }
        matcher.appendTail(buffer);

        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
    
    
    // Format du Chat
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
    	
    	
    	// Vérifie le Préfix & Suffix du Joueur avec LuckPerm
        Player player = event.getPlayer();

        String prefix = "";
        String suffix = "";
        

        if (plugin.getLuckPermsHook() != null) {
            prefix = colorize(plugin.getLuckPermsHook().getPrefix(player));
            suffix = colorize(plugin.getLuckPermsHook().getSuffix(player));
        }         
            

        // Message joueur
        event.setMessage(colorize(event.getMessage()));

        // Format final
        event.setFormat(
                colorize(prefix + player.getName() + suffix + " &7: &f%2$s")
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
