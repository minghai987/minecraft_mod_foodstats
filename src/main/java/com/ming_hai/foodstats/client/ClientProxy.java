package com.ming_hai.foodstats.client;

import com.ming_hai.foodstats.FoodStatsMod;
import com.ming_hai.foodstats.client.screens.FoodGuideScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientProxy {
    public static void openFoodGuideScreen(Player player) {
        var stats = FoodStatsMod.getPlayerStats(player);
        if (stats != null) {
            Minecraft.getInstance().setScreen(new FoodGuideScreen(stats));
        }
    }
}