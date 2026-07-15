// ClientDataHandler.java
package com.ming_hai.foodstats.client;

import com.ming_hai.foodstats.FoodStatsMod;
import com.ming_hai.foodstats.capability.IPlayerStats;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

public class ClientDataHandler {
    
    public static void handleStatsSync(CompoundTag tag) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            IPlayerStats stats = FoodStatsMod.getPlayerStats(minecraft.player);
            if (stats != null) {
                stats.loadNBT(tag);
                FoodStatsMod.LOGGER.debug("客户端已接收并更新玩家数据");
            }
        }
    }
}