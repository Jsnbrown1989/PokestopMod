package com.pokestopmod.util;

import net.luckperms.api.LuckPermsProvider;
import net.minecraft.server.command.ServerCommandSource;

public class PermissionUtil {
    public static boolean hasPermission(ServerCommandSource source, String node) {
        if (source.hasPermissionLevel(4)) return true;
        try {
            var lp = LuckPermsProvider.get();
            var user = lp.getUserManager().getUser(source.getPlayer().getUuid());
            if (user == null) return false;
            var queryOptions = lp.getContextManager().getQueryOptions(user).orElse(null);
            return user.getCachedData().getPermissionData(queryOptions).checkPermission(node).asBoolean();
        } catch (Exception e) {
            return false;
        }
    }
}
