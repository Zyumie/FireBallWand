package fr.zyumie.TPA.Listener;

import fr.zyumie.TPA.TpaMessageUtil;
import fr.zyumie.fireballwand.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TpaManager {

    private final Main plugin;

    private final Map<UUID, Map<UUID, TpaRequest>> incomingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> outgoingRequests = new ConcurrentHashMap<>();
    // Stockage 100% en RAM des derniers temps de téléportation
    private final Map<UUID, Long> lastTeleportTimes = new ConcurrentHashMap<>();

    public TpaManager(Main plugin) {
        this.plugin = plugin;
        // Plus besoin de charger de fichier de cooldowns !
    }

    public long getRemainingCooldown(UUID uuid) {
        Long lastTp = lastTeleportTimes.get(uuid);
        if (lastTp == null) return 0;

        // Récupère le cooldown depuis le gestionnaire de config (10 secondes par défaut)
        long cooldownSeconds = 10L;
        try {
            cooldownSeconds = plugin.getTpaConfigManager().getTpaCooldown();
        } catch (Exception ignored) {}

        long cooldownMillis = cooldownSeconds * 1000L;
        long elapsed = System.currentTimeMillis() - lastTp;
        return elapsed < cooldownMillis ? (cooldownMillis - elapsed + 999L) / 1000L : 0;
    }

    public void recordTeleport(UUID uuid) {
        // Enregistre uniquement en mémoire RAM (disparaît proprement au reboot, ce qui est parfait pour un cooldown)
        lastTeleportTimes.put(uuid, System.currentTimeMillis());
    }

    public boolean hasPendingRequestFrom(UUID targetId, UUID senderId) {
        Map<UUID, TpaRequest> map = incomingRequests.get(targetId);
        return map != null && map.containsKey(senderId);
    }

    public TpaRequest getLatestRequest(UUID targetId) {
        Map<UUID, TpaRequest> map = incomingRequests.get(targetId);
        if (map == null || map.isEmpty()) return null;
        int expSec = plugin.getTpaConfigManager().getRequestExpirationSeconds();
        TpaRequest latest = null;
        for (TpaRequest req : map.values()) {
            if (!req.isExpired(expSec)) {
                if (latest == null || req.getCreatedAt() > latest.getCreatedAt()) {
                    latest = req;
                }
            }
        }
        return latest;
    }

    public TpaRequest getRequestFrom(UUID targetId, UUID senderId) {
        Map<UUID, TpaRequest> map = incomingRequests.get(targetId);
        if (map == null) return null;
        TpaRequest req = map.get(senderId);
        if (req != null && req.isExpired(plugin.getTpaConfigManager().getRequestExpirationSeconds())) {
            removeRequest(targetId, senderId);
            return null;
        }
        return req;
    }

    public void sendRequest(Player sender, Player target, boolean tpHere) {
        UUID senderId = sender.getUniqueId();
        UUID targetId = target.getUniqueId();

        if (outgoingRequests.containsKey(senderId)) {
            UUID prevTarget = outgoingRequests.get(senderId);
            removeRequest(prevTarget, senderId);
        }

        TpaRequest request = new TpaRequest(senderId, targetId, tpHere);
        incomingRequests.computeIfAbsent(targetId, k -> new ConcurrentHashMap<>()).put(senderId, request);
        outgoingRequests.put(senderId, targetId);

        int expSec = plugin.getTpaConfigManager().getRequestExpirationSeconds();

        String sentMsgKey = tpHere ? "request-sent-here" : "request-sent";
        String sentMsg = plugin.getTpaConfigManager().getMessage(sentMsgKey, "&aDemande envoyée à &e{target}&a.")
                .replace("{target}", target.getName()).replace("{expire}", String.valueOf(expSec));
        TpaMessageUtil.sendMessage(sender, plugin.getTpaConfigManager().getPrefix(), sentMsg);

        String recvMsgKey = tpHere ? "request-received-here" : "request-received";
        String receivedMsg = plugin.getTpaConfigManager().getMessage(recvMsgKey, "&e{sender} &6souhaite se téléporter !")
                .replace("{sender}", sender.getName());
        TpaMessageUtil.sendMessage(target, plugin.getTpaConfigManager().getPrefix(), receivedMsg);
        TpaMessageUtil.sendTpaRequestButtons(target, plugin.getTpaConfigManager().getPrefix(), sender.getName());

        new BukkitRunnable() {
            @Override
            public void run() {
                Map<UUID, TpaRequest> map = incomingRequests.get(targetId);
                if (map != null && map.get(senderId) == request) {
                    removeRequest(targetId, senderId);
                    Player s = Bukkit.getPlayer(senderId);
                    if (s != null && s.isOnline()) {
                        TpaMessageUtil.sendMessage(s, plugin.getTpaConfigManager().getPrefix(),
                                plugin.getTpaConfigManager().getMessage("request-expired-sender", "&cLa demande a expiré.").replace("{target}", target.getName()));
                    }
                    Player t = Bukkit.getPlayer(targetId);
                    if (t != null && t.isOnline()) {
                        TpaMessageUtil.sendMessage(t, plugin.getTpaConfigManager().getPrefix(),
                                plugin.getTpaConfigManager().getMessage("request-expired-target", "&cLa demande a expiré.").replace("{sender}", sender.getName()));
                    }
                }
            }
        }.runTaskLater(plugin, expSec * 20L);
    }

    public void removeRequest(UUID targetId, UUID senderId) {
        Map<UUID, TpaRequest> map = incomingRequests.get(targetId);
        if (map != null) {
            map.remove(senderId);
            if (map.isEmpty()) incomingRequests.remove(targetId);
        }
        outgoingRequests.remove(senderId);
    }

    public UUID cancelOutgoingRequest(UUID senderId) {
        UUID targetId = outgoingRequests.remove(senderId);
        if (targetId != null) {
            Map<UUID, TpaRequest> map = incomingRequests.get(targetId);
            if (map != null) {
                map.remove(senderId);
                if (map.isEmpty()) incomingRequests.remove(targetId);
            }
        }
        return targetId;
    }

    public void cleanupPlayer(UUID uuid) {
        cancelOutgoingRequest(uuid);
        Map<UUID, TpaRequest> received = incomingRequests.remove(uuid);
        if (received != null) {
            for (UUID senderId : received.keySet()) outgoingRequests.remove(senderId);
        }
    }
}