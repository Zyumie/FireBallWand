package fr.zyumie.Listener;

import fr.zyumie.fireballwand.Main;

import net.md_5.bungee.api.ChatColor;
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
	
    
    // Convertit un texte contenant des codes hex (#RRGGBB) en ChatColor utilisable
    public static String translateHexColors(String text) {
    	
        // Exemple : "&fHello #FF0000World" devient "Hello" en blanc et "World" en rouge
        final String HEX_PATTERN = "#[a-fA-F0-9]{6}";
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(HEX_PATTERN);
        java.util.regex.Matcher matcher = pattern.matcher(text);

        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hexColor = matcher.group();
            matcher.appendReplacement(buffer, ChatColor.of(hexColor) + "");
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
            prefix = plugin.getLuckPermsHook().getPrefix(event.getPlayer());
            suffix = plugin.getLuckPermsHook().getSuffix(event.getPlayer());
            
            
            // Traduire les codes hex en couleur RGB
            prefix = translateHexColors(prefix);
            suffix = translateHexColors(suffix);
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
