package fr.zyumie.Commandes;

import fr.zyumie.Chat.ChatManager;
import fr.zyumie.fireballwand.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class kickarmor implements Listener, CommandExecutor {

    private final Main plugin;
    private final NamespacedKey armorKey;

    public kickarmor(Main plugin) {
        this.plugin = plugin;
        this.armorKey = new NamespacedKey(plugin, "armor_id");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player wearer = event.getPlayer();

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        ItemStack chest = wearer.getInventory().getChestplate();
        if (chest == null || chest.getType() != Material.DIAMOND_CHESTPLATE) return;

        ItemMeta meta = chest.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        String id = data.get(armorKey, PersistentDataType.STRING);
        if (id == null || !id.equals("KICK_ARMOR")) return;

        double radius = plugin.getCustomCheatConfig().getDouble("kickarmor.radius", 3.0);
        String kickMsg = ChatManager.colorize(
                plugin.getCustomCheatConfig().getString("kickarmor.kick-message", "&cTrop près d'un joueur protégé."));

        for (Entity entity : wearer.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player target) {
                if (target.equals(wearer) || target.isOp()) continue;

                target.kickPlayer(kickMsg);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Seul un joueur peut utiliser cette commande.");
            return true;
        }

        if (!player.hasPermission("fireballwand.kickarmor")) {
            player.sendMessage("§cTu n'as pas la permission d'utiliser cet item.");
            return true;
        }

        String name = ChatManager.colorize(
                plugin.getCustomCheatConfig().getString("kickarmor.name", "&cPlastron du Kick"));

        ItemStack chest = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta meta = chest.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.getPersistentDataContainer().set(armorKey, PersistentDataType.STRING, "KICK_ARMOR");
            chest.setItemMeta(meta);
        }

        player.getInventory().addItem(chest);
        player.sendMessage("§aTu as reçu le " + name + "§a !");
        return true;
    }
}