package com.ming_hai.foodstats.client;

import com.ming_hai.foodstats.FoodStatsMod;
import com.ming_hai.foodstats.capability.IPlayerStats;
import com.ming_hai.foodstats.client.screens.FoodGuideScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class ClientProxy {
    public void openFoodGuideScreen(Player player) {
        player.getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
            Minecraft.getInstance().setScreen(new FoodGuideScreen(stats));
        });
    }
}