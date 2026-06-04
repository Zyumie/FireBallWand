package fr.zyumie.Commandes;

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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class freezewand implements Listener, CommandExecutor {

    private final NamespacedKey wandKey;
    private final Set<UUID> frozenPlayers = new HashSet<>();

    public freezewand(JavaPlugin plugin) {
        this.wandKey = new NamespacedKey(plugin, "freeze_wand");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /* ================== COMMAND ================== */

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] arg) {

        if (!(sender instanceof Player player)) return true;

        // /unfreezewand-all
        if (command.getName().equalsIgnoreCase("unfreezewand-all")) {

            if (!player.hasPermission("fireballwand.unfreezewand")) {
                player.sendMessage("§cTu n’as pas la permission d’utiliser cet item.");
                return true;
            }

            frozenPlayers.clear();
            Bukkit.broadcastMessage("§b❄ Tous les joueurs ont été dégelés !");
            return true;
        }

        // Vérifie la Perm pour donner la wand
        if (!player.hasPermission("fireballwand.freezewand")) {
            player.sendMessage("§cTu n’as pas la permission d’utiliser cet item.");
            return true;
        }

        // Donne la FreezeWand au joueur
        giveWand(player);
        return true;
    }

    public void giveWand(Player player) {

        ItemStack wand = new ItemStack(Material.STONE_SWORD);
        ItemMeta meta = wand.getItemMeta();

        meta.setDisplayName("§bBâton de Glace");
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.STRING, "FREEZE_WAND");

        wand.setItemMeta(meta);

        player.getInventory().addItem(wand);
        player.sendMessage("§aTu as reçu le Bâton de Glace !");
    }

    /* ================== EVENTS ================== */

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        // Vérifie si le damager tient bien la FreezeWand
        ItemStack hand = damager.getInventory().getItemInMainHand();
        if (!isFreezeWand(hand)) return;

        // OP immunisé
        if (target.isOp()) {
            damager.sendMessage("§c❌ Impossible de geler un OP.");
            return;
        }

        event.setCancelled(true);

        UUID uuid = target.getUniqueId();
        if (frozenPlayers.remove(uuid)) {
            target.sendMessage("§aTu es dégelé.");
            damager.sendMessage("§bJoueur dégelé.");
        } else {
            frozenPlayers.add(uuid);
            target.sendMessage("§cTu es gelé.");
            damager.sendMessage("§bJoueur gelé.");
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!frozenPlayers.contains(event.getPlayer().getUniqueId())) return;

        if (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getZ() != event.getTo().getZ()) {
            event.setCancelled(true);
        }
    }

    /* ================== UTILS ================== */

    private boolean isFreezeWand(ItemStack item) {
        if (item == null || item.getType() != Material.STONE_SWORD) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        // Correct PersistentDataType ici
        return meta.getPersistentDataContainer().has(wandKey, PersistentDataType.STRING);
    }

}
