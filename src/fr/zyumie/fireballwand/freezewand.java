package fr.zyumie.fireballwand;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class freezewand implements Listener, CommandExecutor {

    private final JavaPlugin plugin;
    private final NamespacedKey wandKey;
    private final Set<UUID> frozenPlayers = new HashSet<>();

    public freezewand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.wandKey = new NamespacedKey(plugin, "freeze_wand");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void giveWand(Player player) {
        if (!player.isOp()) {
            player.sendMessage("§cCommande réservée aux administrateurs.");
            return;
        }

        ItemStack wand = new ItemStack(Material.STONE_SWORD);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName("§bBâton de Glace");
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.STRING, "FREEZE_WAND");
        wand.setItemMeta(meta);

        player.getInventory().addItem(wand);
        player.sendMessage("§aTu as reçu le Bâton de Glace !");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        giveWand(player);
        return true;
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        ItemStack item = damager.getInventory().getItemInMainHand();
        if (item.getType() != Material.STONE_SWORD) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        String id = data.get(wandKey, PersistentDataType.STRING);
        if (id == null || !id.equals("FREEZE_WAND")) return;

        event.setCancelled(true);

        if (frozenPlayers.contains(target.getUniqueId())) {
            frozenPlayers.remove(target.getUniqueId());
            target.sendMessage("§aTu es dégelé !");
            damager.sendMessage("§bTu as dégelé §e" + target.getName());
        } else {
            frozenPlayers.add(target.getUniqueId());
            target.sendMessage("§cTu es gelé !");
            damager.sendMessage("§bTu as gelé §e" + target.getName());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (frozenPlayers.contains(player.getUniqueId())) {
            event.setTo(event.getFrom());
        }
    }
}
