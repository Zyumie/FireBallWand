package fr.zyumie.TPA.Listener;

import java.util.UUID;

public class TpaRequest {

    private final UUID senderId;
    private final UUID targetId;
    private final boolean tpHere;
    private final long createdAt;

    public TpaRequest(UUID senderId, UUID targetId, boolean tpHere) {
        this.senderId = senderId;
        this.targetId = targetId;
        this.tpHere = tpHere;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getSenderId() { return senderId; }
    public UUID getTargetId() { return targetId; }
    public boolean isTpHere() { return tpHere; }
    public long getCreatedAt() { return createdAt; }

    public boolean isExpired(int expirationSeconds) {
        return (System.currentTimeMillis() - createdAt) > (expirationSeconds * 1000L);
    }
}