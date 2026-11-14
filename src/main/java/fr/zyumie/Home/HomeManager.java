package fr.zyumie.Home;

import fr.zyumie.fireballwand.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stockage des homes : UN fichier par joueur (plugins/FireBallWand/data/homes/<uuid>.yml).
 *
 * Pensé pour le TPS :
 *  - lecture hors du thread principal, à la pré-connexion du joueur ;
 *  - écriture asynchrone, uniquement si le joueur a modifié ses homes, regroupée sur 1 seconde ;
 *  - on n'écrit que le petit fichier du joueur concerné (jamais un fichier global) ;
 *  - rien n'est écrit à la déconnexion si rien n'a changé.
 */
public final class HomeManager {

    private final Main plugin;
    private final File dir;
    private final ExecutorService io;
    private final Object writeLock = new Object();

    /** UUID -> (nom du home -> données). Les maps internes ne sont modifiées que sur le thread principal. */
    private final Map<UUID, Map<String, HomeData>> homes = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private final Set<UUID> saveQueued = ConcurrentHashMap.newKeySet();

    // Clés partagées par le menu et son écouteur
    public final NamespacedKey actionKey;
    public final NamespacedKey homeNameKey;

    public HomeManager(Main plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "data/homes");
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("[Home] Impossible de créer le dossier " + dir.getPath());
        }
        this.io = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "FireBallWand-Homes-IO");
            t.setDaemon(true);
            return t;
        });
        this.actionKey = new NamespacedKey(plugin, "gui_action");
        this.homeNameKey = new NamespacedKey(plugin, "home_name");
    }

    public File file(UUID uuid) {
        return new File(dir, uuid + ".yml");
    }

    // ── Lecture ──────────────────────────────────────────────────────────────

    /** Lit le fichier du joueur (map vide s'il n'existe pas). Utilisable hors du thread principal. */
    public Map<String, HomeData> readFromDisk(UUID uuid) {
        Map<String, HomeData> out = new LinkedHashMap<>();
        File f = file(uuid);
        if (!f.exists()) return out;

        try {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
            for (Map<?, ?> m : yml.getMapList("homes")) {
                Object name = m.get("name");
                Object world = m.get("world");
                if (name == null || world == null) continue;
                out.put(String.valueOf(name).toLowerCase(), new HomeData(
                        String.valueOf(world),
                        num(m, "x", 0), num(m, "y", 64), num(m, "z", 0),
                        (float) num(m, "yaw", 0), (float) num(m, "pitch", 0),
                        (long) num(m, "set-time", System.currentTimeMillis())));
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[Home] Fichier illisible pour " + uuid + " : " + e.getMessage());
        }
        return out;
    }

    /**
     * Lecture séquencée après les écritures déjà en file : à utiliser à la pré-connexion,
     * pour qu'un joueur qui se reconnecte aussitôt retrouve ses derniers changements.
     */
    public Map<String, HomeData> readOrdered(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> readFromDisk(uuid), io).join();
    }

    public void cache(UUID uuid, Map<String, HomeData> data) {
        homes.put(uuid, data);
    }

    /** Repli (ex : /reload) : lecture synchrone du seul petit fichier du joueur. */
    private Map<String, HomeData> loaded(UUID uuid) {
        return homes.computeIfAbsent(uuid, this::readFromDisk);
    }

    private static double num(Map<?, ?> map, String key, double def) {
        return map.get(key) instanceof Number n ? n.doubleValue() : def;
    }

    // ── Accès ────────────────────────────────────────────────────────────────

    public Map<String, HomeData> getHomes(UUID uuid) {
        return Collections.unmodifiableMap(loaded(uuid));
    }

    public boolean hasHome(UUID uuid, String name) {
        return loaded(uuid).containsKey(name);
    }

    public HomeData getHome(UUID uuid, String name) {
        return loaded(uuid).get(name);
    }

    public int getHomeCount(UUID uuid) {
        return loaded(uuid).size();
    }

    public void setHome(UUID uuid, String name, Location loc) {
        loaded(uuid).put(name, HomeData.of(loc, System.currentTimeMillis()));
        markDirty(uuid);
    }

    public void deleteHome(UUID uuid, String name) {
        if (loaded(uuid).remove(name) != null) markDirty(uuid);
    }

    /** Nombre de homes autorisés (1 à 5) : fireballwand.home.max.N, ou l'ancienne permission home.max.N. */
    public int getMaxHomes(Player player) {
        for (int i = 5; i >= 1; i--) {
            if (player.hasPermission("fireballwand.home.max." + i) || player.hasPermission("home.max." + i)) {
                return i;
            }
        }
        return 1;
    }

    // ── Écriture ─────────────────────────────────────────────────────────────

    private void markDirty(UUID uuid) {
        dirty.add(uuid);
        // Regroupe les changements rapprochés (ex : plusieurs suppressions dans le menu) en une écriture
        if (saveQueued.add(uuid)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                saveQueued.remove(uuid);
                flushAsync(uuid);
            }, 20L);
        }
    }

    private void flushAsync(UUID uuid) {
        if (!dirty.remove(uuid)) return;
        Map<String, HomeData> data = homes.get(uuid);
        if (data == null) return;
        Map<String, HomeData> snapshot = new LinkedHashMap<>(data); // copie prise sur le thread principal
        io.execute(() -> write(uuid, snapshot));
    }

    private void flushNow(UUID uuid) {
        if (!dirty.remove(uuid)) return;
        Map<String, HomeData> data = homes.get(uuid);
        if (data != null) write(uuid, new LinkedHashMap<>(data));
    }

    /** Écrit le fichier d'un joueur (fichier temporaire puis déplacement atomique ; supprimé si vide). */
    public void write(UUID uuid, Map<String, HomeData> data) {
        synchronized (writeLock) {
            File target = file(uuid);
            try {
                if (data.isEmpty()) {
                    Files.deleteIfExists(target.toPath());
                    return;
                }

                List<Map<String, Object>> list = new ArrayList<>();
                for (Map.Entry<String, HomeData> e : data.entrySet()) {
                    HomeData h = e.getValue();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.getKey());
                    m.put("world", h.world());
                    m.put("x", h.x());
                    m.put("y", h.y());
                    m.put("z", h.z());
                    m.put("yaw", h.yaw());
                    m.put("pitch", h.pitch());
                    m.put("set-time", h.setTime());
                    list.add(m);
                }
                YamlConfiguration yml = new YamlConfiguration();
                yml.set("homes", list);

                File tmp = new File(dir, uuid + ".yml.tmp");
                yml.save(tmp);
                try {
                    Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("[Home] Écriture impossible pour " + uuid + " : " + e.getMessage());
            }
        }
    }

    /** Déconnexion : n'écrit que si une modification n'a pas encore été enregistrée (rare). */
    public void unload(UUID uuid) {
        flushNow(uuid);
        homes.remove(uuid);
    }

    /** Arrêt du plugin : enregistre ce qui reste puis attend la fin des écritures en cours. */
    public void saveAll() {
        for (UUID uuid : new ArrayList<>(dirty)) flushNow(uuid);
        io.shutdown();
        try {
            if (!io.awaitTermination(5, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("[Home] Des écritures étaient encore en cours à l'arrêt.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
