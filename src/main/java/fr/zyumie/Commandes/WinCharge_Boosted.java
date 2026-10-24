package fr.zyumie.Commandes;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class WinCharge_Boosted implements Listener, CommandExecutor {

    private final JavaPlugin plugin;
    private final NamespacedKey winchargeKey;
    private final NamespacedKey winchargeDeathKey; // Tag temporaire pour conserver l'inventaire à la mort
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public WinCharge_Boosted(JavaPlugin plugin) {
        this.plugin = plugin;
        this.winchargeKey = new NamespacedKey(plugin, "wincharge_boosted");
        this.winchargeDeathKey = new NamespacedKey(plugin, "wincharge_death_tag");
    }

    // --- CRÉATION DE L'ITEM ---
    public ItemStack createWinchargeItem() {
        ItemStack item = new ItemStack(Material.WIND_CHARGE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = plugin.getConfig().getString("wincharge.name", "&eWincharge &6Boosted");
            meta.setDisplayName(formatColor(name));

            List<String> lore = plugin.getConfig().getStringList("wincharge.lore");
            List<String> formattedLore = new ArrayList<>();
            for (String line : lore) {
                formattedLore.add(formatColor(line));
            }
            meta.setLore(formattedLore);

            meta.getPersistentDataContainer().set(winchargeKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }

        return item;
    }

    // --- DÉTECTION DU TIR & COOLDOWN ---
    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof WindCharge windCharge)) return;
        if (!(windCharge.getShooter() instanceof Player player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isWinchargeItem(item)) {
            item = player.getInventory().getItemInOffHand();
            if (!isWinchargeItem(item)) return;
        }

        // Vérification de la permission de tir
        if (!player.hasPermission("fireballwand.wincharge_boosted")) {
            player.sendMessage(formatColor("&c❌ Tu n'as pas la permission d'utiliser la Wincharge Boosted."));
            event.setCancelled(true);
            return;
        }

        int cooldownSeconds = plugin.getConfig().getInt("wincharge.cooldown", 5);
        long currentTime = System.currentTimeMillis();

        if (cooldowns.containsKey(player.getUniqueId())) {
            long lastUse = cooldowns.get(player.getUniqueId());
            long timePassed = (currentTime - lastUse) / 1000;

            if (timePassed < cooldownSeconds) {
                long timeLeft = cooldownSeconds - timePassed;
                String msg = plugin.getConfig().getString("wincharge.messages.cooldown", "&c❌ Attends encore %seconds%s !")
                        .replace("%seconds%", String.valueOf(timeLeft));
                player.sendMessage(formatColor(msg));
                event.setCancelled(true);
                return;
            }
        }

        cooldowns.put(player.getUniqueId(), currentTime);
        windCharge.getPersistentDataContainer().set(winchargeKey, PersistentDataType.BYTE, (byte) 1);
    }

    // --- IMPACT : EXPLOSION & MORT UNIQUEMENT DES JOUEURS ---
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof WindCharge windCharge)) return;

        PersistentDataContainer pdc = windCharge.getPersistentDataContainer();
        if (!pdc.has(winchargeKey, PersistentDataType.BYTE)) return;

        Location loc = windCharge.getLocation();
        if (event.getHitBlock() != null) {
            loc = event.getHitBlock().getLocation().add(0.5, 1.0, 0.5);
        } else if (event.getHitEntity() != null) {
            loc = event.getHitEntity().getLocation();
        }

        double radius = plugin.getConfig().getDouble("wincharge.radius", 2.5);
        int particleCount = plugin.getConfig().getInt("wincharge.particles-count", 3);

        // Effets visuels et sonores (aucun dégât aux blocs)
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, particleCount, 0.2, 0.2, 0.2, 0);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        Collection<Entity> nearby = loc.getWorld().getNearbyEntities(loc, radius, radius, radius);
        Player shooter = (windCharge.getShooter() instanceof Player) ? (Player) windCharge.getShooter() : null;

        for (Entity entity : nearby) {
            // 🎯 UNIQUEMENT LES JOUEURS (les mobs sont ignorés)
            if (entity instanceof Player victim) {

                // Empêche le tireur de se tuer lui-même
                if (shooter != null && victim.equals(shooter)) continue;

                // 🛡️ BYPASS : Les OP ou les joueurs avec la permission ne meurent pas !
                if (victim.isOp() || victim.hasPermission("fireballwand.wincharge.bypass")) {
                    continue;
                }

                // Marque la victime pour activer le KeepInventory à sa mort
                victim.getPersistentDataContainer().set(winchargeDeathKey, PersistentDataType.BYTE, (byte) 1);

                String msg = plugin.getConfig().getString("wincharge.messages.killed", "&c⚡ Tu as été exterminé !");
                victim.sendMessage(formatColor(msg));

                victim.setHealth(0.0);
            }
        }

        windCharge.remove();
        event.setCancelled(true);
    }

    // --- GESTION DU KEEP INVENTORY ---
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PersistentDataContainer pdc = player.getPersistentDataContainer();

        if (pdc.has(winchargeDeathKey, PersistentDataType.BYTE)) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
            pdc.remove(winchargeDeathKey); // Retire le tag
        }
    }

    // --- COMMANDE : /wincharge_boosted ---
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Seul un joueur peut exécuter cette commande.");
            return true;
        }

        if (!player.hasPermission("fireballwand.wincharge_boosted")) {
            player.sendMessage(formatColor("&c❌ Tu n'as pas la permission d'utiliser cette commande."));
            return true;
        }

        player.getInventory().addItem(createWinchargeItem());
        String msg = plugin.getConfig().getString("wincharge.messages.given", "&aTu as reçu la Wincharge Boosted !");
        player.sendMessage(formatColor(msg));

        return true;
    }

    private boolean isWinchargeItem(ItemStack item) {
        if (item == null || item.getType() != Material.WIND_CHARGE || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(winchargeKey, PersistentDataType.BYTE);
    }

    private String formatColor(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}