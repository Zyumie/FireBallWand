package fr.zyumie.Staff.Listener;

import fr.zyumie.fireballwand.Main;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.UUID;

/**
 * Sauvegarde l'inventaire d'origine d'un joueur AVANT qu'il ne passe en mode staff.
 *
 * Sécurité :
 *  - l'écriture se fait sur disque (fichier temporaire puis déplacement atomique) AVANT
 *    que l'inventaire soit vidé : si l'écriture échoue, le mode staff est annulé ;
 *  - le fichier n'est supprimé qu'une fois l'inventaire entièrement restauré ;
 *  - un fichier présent à la connexion = crash pendant un mode staff → restauration auto.
 *
 * Fichiers : plugins/FireBallWand/data/staff/<uuid>.yml
 */
public class StaffInventoryStore {

    private final Main plugin;
    private final File dir;

    public StaffInventoryStore(Main plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "data/staff");
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("Impossible de créer le dossier " + dir.getPath());
        }
    }

    private File file(UUID id) {
        return new File(dir, id + ".yml");
    }

    public boolean hasSnapshot(UUID id) {
        return file(id).exists();
    }

    // ── Sauvegarde ───────────────────────────────────────────────────────────

    /** @return true si la sauvegarde est bien écrite sur disque. */
    public boolean save(Player player, boolean saveXp) {
        YamlConfiguration yml = new YamlConfiguration();
        PlayerInventory inv = player.getInventory();

        ItemStack[] storage = inv.getStorageContents();
        for (int i = 0; i < storage.length; i++) put(yml, "storage." + i, storage[i]);

        ItemStack[] armor = inv.getArmorContents();
        for (int i = 0; i < armor.length; i++) put(yml, "armor." + i, armor[i]);

        put(yml, "offhand", inv.getItemInOffHand());
        yml.set("allow-flight", player.getAllowFlight());

        if (saveXp) {
            yml.set("xp.level", player.getLevel());
            yml.set("xp.exp", (double) player.getExp());
        }

        File target = file(player.getUniqueId());
        File tmp = new File(dir, player.getUniqueId() + ".yml.tmp");

        try {
            yml.save(tmp);
            try {
                Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Sauvegarde d'inventaire impossible pour " + player.getName() + " : " + e.getMessage());
            return false;
        }
    }

    private void put(YamlConfiguration yml, String path, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        yml.set(path, Base64.getEncoder().encodeToString(item.serializeAsBytes()));
    }

    // ── Restauration ─────────────────────────────────────────────────────────

    /**
     * Restaure l'inventaire d'origine. Tout est LU avant de toucher à l'inventaire :
     * en cas de fichier illisible, rien n'est modifié et le fichier est conservé.
     *
     * @return true si l'inventaire a été restauré (fichier supprimé)
     */
    public boolean restore(Player player) {
        File f = file(player.getUniqueId());
        if (!f.exists()) return false;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);

        try {
            ItemStack[] storage = new ItemStack[36];
            for (int i = 0; i < storage.length; i++) storage[i] = read(yml, "storage." + i);

            ItemStack[] armor = new ItemStack[4];
            for (int i = 0; i < armor.length; i++) armor[i] = read(yml, "armor." + i);

            ItemStack offhand = read(yml, "offhand");

            // Lecture complète réussie : on peut modifier l'inventaire
            player.closeInventory();
            player.setItemOnCursor(null);

            PlayerInventory inv = player.getInventory();
            inv.clear();
            inv.setStorageContents(storage);
            inv.setArmorContents(armor);
            inv.setItemInOffHand(offhand);

            if (yml.contains("xp.level")) {
                player.setLevel(yml.getInt("xp.level"));
                player.setExp((float) yml.getDouble("xp.exp"));
            }

            GameMode gm = player.getGameMode();
            if (gm == GameMode.SURVIVAL || gm == GameMode.ADVENTURE) {
                boolean allow = yml.getBoolean("allow-flight", false);
                player.setAllowFlight(allow);
                if (!allow) player.setFlying(false);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Restauration impossible pour " + player.getName()
                    + " — fichier conservé : " + f.getPath() + " (" + e.getMessage() + ")");
            return false;
        }

        try {
            Files.deleteIfExists(f.toPath());
        } catch (IOException e) {
            plugin.getLogger().warning("Inventaire restauré mais fichier non supprimé : " + f.getPath());
        }
        return true;
    }

    private ItemStack read(YamlConfiguration yml, String path) {
        String b64 = yml.getString(path);
        if (b64 == null) return null;
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(b64));
    }
}
