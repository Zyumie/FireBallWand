package fr.zyumie.Commandes;

import fr.zyumie.Listener.ChatManager;
import fr.zyumie.fireballwand.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class freezewand implements Listener, CommandExecutor {

    private final Main plugin;
    private final NamespacedKey wandKey;
    private final Set<UUID> frozenPlayers = new HashSet<>();

    public freezewand(Main plugin) {
        this.plugin = plugin;
        this.wandKey = new NamespacedKey(plugin, "freeze_wand");
    }

    // ── Helpers config ───────────────────────────────────────────────────────

    private String wandName() {
        return ChatManager.colorize(
                plugin.getConfig().getString("freezewand.name", "&bBâton de Glace"));
    }

    private String msg(String key, String fallback) {
        return ChatManager.colorize(plugin.getConfig().getString("freezewand.messages." + key, fallback));
    }

    // ── Commande ─────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        // /unfreezewand-all
        if (command.getName().equalsIgnoreCase("unfreezewand-all")) {
            if (!player.hasPermission("fireballwand.unfreezewand")) {
                player.sendMessage("§cTu n'as pas la permission d'utiliser cette commande.");
                return true;
            }
            frozenPlayers.clear();
            Bukkit.broadcastMessage(msg("all-unfrozen", "&b❄ Tous les joueurs ont été dégelés !"));
            return true;
        }

        // /freezewand
        if (!player.hasPermission("fireballwand.freezewand")) {
            player.sendMessage("§cTu n'as pas la permission d'utiliser cet item.");
            return true;
        }

        giveWand(player);
        return true;
    }

    public void giveWand(Player player) {
        ItemStack wand = new ItemStack(Material.STONE_SWORD);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName(wandName());
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.STRING, "FREEZE_WAND");
        wand.setItemMeta(meta);

        player.getInventory().addItem(wand);
        player.sendMessage("§aTu as reçu le " + wandName() + "§a !");
    }

    // ── Évènements ───────────────────────────────────────────────────────────

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof Player target)) return;
        if (!isFreezeWand(damager.getInventory().getItemInMainHand())) return;

        if (target.isOp()) {
            damager.sendMessage(msg("op-immune", "&c❌ Impossible de geler un OP."));
            return;
        }

        event.setCancelled(true);

        UUID uuid = target.getUniqueId();
        if (frozenPlayers.remove(uuid)) {
            target.sendMessage(msg("unfrozen", "&aTu es dégelé."));
            damager.sendMessage(msg("target-unfrozen", "&bJoueur dégelé."));
        } else {
            frozenPlayers.add(uuid);
            target.sendMessage(msg("frozen", "&cTu es gelé."));
            damager.sendMessage(msg("target-frozen", "&bJoueur gelé."));
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

    // ── Utils ────────────────────────────────────────────────────────────────

    private boolean isFreezeWand(ItemStack item) {
        if (item == null || item.getType() != Material.STONE_SWORD) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(wandKey, PersistentDataType.STRING);
    }
}
