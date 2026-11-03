package fr.zyumie.Staff.GodMod;

import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GodManager {

    private final Set<UUID> gods = ConcurrentHashMap.newKeySet();

    public boolean isGod(Player player) {
        return gods.contains(player.getUniqueId());
    }

    /** @return true si l'état a changé */
    public boolean enable(Player player) {
        return gods.add(player.getUniqueId());
    }

    /** @return true si l'état a changé */
    public boolean disable(Player player) {
        return gods.remove(player.getUniqueId());
    }

    /** @return le nouvel état (true = godmod actif) */
    public boolean toggle(Player player) {
        if (disable(player)) return false;
        enable(player);
        return true;
    }

    public void clear() {
        gods.clear();
    }
}
