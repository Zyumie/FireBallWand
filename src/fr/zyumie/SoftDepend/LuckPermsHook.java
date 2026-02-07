package fr.zyumie.SoftDepend;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.entity.Player;

public class LuckPermsHook {

    private final LuckPerms api;

    public LuckPermsHook() {
        this.api = LuckPermsProvider.get();
    }

    public String getPrefix(Player player) {
        User user = api.getUserManager().getUser(player.getUniqueId());
        if (user == null) return "";

        CachedMetaData meta = user.getCachedData().getMetaData();
        return meta.getPrefix() != null ? meta.getPrefix() : "";
    }

    public String getSuffix(Player player) {
        User user = api.getUserManager().getUser(player.getUniqueId());
        if (user == null) return "";

        CachedMetaData meta = user.getCachedData().getMetaData();
        return meta.getSuffix() != null ? meta.getSuffix() : "";
    }
}
