package fr.zyumie.Commandes;

import fr.zyumie.Chat.ChatManager;
import fr.zyumie.fireballwand.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private String getWandName() {
        return ChatManager.colorize(
                plugin.getCustomCheatConfig().getString("freezewand.name", "&bBâton de Glace"));
    }

    private String getMsg(String key, String fallback) {
        return ChatManager.colorize(
                plugin.getCustomCheatConfig().getString("freezewand.messages." + key, fallback));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Seul un joueur peut exécuter cette commande.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("unfreezewand-all")) {
            if (!player.hasPermission("fireballwand.unfreezewand")) {
                player.sendMessage(getMsg("no-perm-cmd", "§cTu n'as pas la permission d'utiliser cette commande."));
                return true;
            }

            frozenPlayers.clear();
            Bukkit.broadcastMessage(getMsg("all-unfrozen", "&b❄ Tous les joueurs ont été dégelés !"));
            return true;
        }

        if (!player.hasPermission("fireballwand.freezewand")) {
            player.sendMessage(getMsg("no-perm-item", "§cTu n'as pas la permission d'utiliser cet item."));
            return true;
        }

        giveWand(player);
        return true;
    }

    public void giveWand(Player player) {
        ItemStack wand = new ItemStack(Material.STONE_SWORD);
        ItemMeta meta = wand.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(getWandName());
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.STRING, "FREEZE_WAND");
            wand.setItemMeta(meta);
        }

        player.getInventory().addItem(wand);
        player.sendMessage("§aTu as reçu le " + getWandName() + "§a !");
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        ItemStack itemInHand = damager.getInventory().getItemInMainHand();
        if (!isFreezeWand(itemInHand)) return;

        event.setCancelled(true);

        if (target.isOp()) {
            damager.sendMessage(getMsg("op-immune", "&c❌ Impossible de geler un OP."));
            return;
        }

        UUID uuid = target.getUniqueId();
        if (frozenPlayers.contains(uuid)) {
            frozenPlayers.remove(uuid);
            target.sendMessage(getMsg("unfrozen", "&aTu es dégelé."));
            damager.sendMessage(getMsg("target-unfrozen", "&bJoueur dégelé."));
        } else {
            frozenPlayers.add(uuid);
            target.sendMessage(getMsg("frozen", "&cTu es gelé."));
            damager.sendMessage(getMsg("target-frozen", "&bJoueur gelé."));
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!frozenPlayers.contains(player.getUniqueId())) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) return;

        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            Location freezeLoc = from.clone();
            freezeLoc.setYaw(to.getYaw());
            freezeLoc.setPitch(to.getPitch());
            event.setTo(freezeLoc);
        }
    }

    private boolean isFreezeWand(ItemStack item) {
        if (item == null || item.getType() != Material.STONE_SWORD) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return meta.getPersistentDataContainer().has(wandKey, PersistentDataType.STRING);
    }
}