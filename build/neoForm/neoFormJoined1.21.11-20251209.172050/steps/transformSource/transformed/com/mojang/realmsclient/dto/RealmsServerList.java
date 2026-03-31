package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record RealmsServerList(@SerializedName("servers") List<RealmsServer> servers) implements ReflectionBasedSerialization {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static RealmsServerList parse(GuardedSerializer serializer, String json) {
        try {
            RealmsServerList realmsserverlist = serializer.fromJson(json, RealmsServerList.class);
            if (realmsserverlist != null) {
                realmsserverlist.servers.forEach(RealmsServer::finalize);
                return realmsserverlist;
            }

            LOGGER.error("Could not parse McoServerList: {}", json);
        } catch (Exception exception) {
            LOGGER.error("Could not parse McoServerList", (Throwable)exception);
        }

        return new RealmsServerList(List.of());
    }
}
