package com.ming_hai.foodstats.client;

import com.ming_hai.foodstats.FoodStatsMod;
import com.ming_hai.foodstats.capability.IPlayerStats;
import com.ming_hai.foodstats.client.screens.FoodGuideScreen;
import com.ming_hai.foodstats.client.screens.RemoteServerConfigBlockedScreen;
import com.ming_hai.foodstats.config.Config;
import com.ming_hai.foodstats.compat.JEIIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class ClientHooks {
    private ClientHooks() {
    }

    public static void registerConfigScreen(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
            (container, parent) -> {
                if (isConnectedToRemoteServer()) {
                    return new RemoteServerConfigBlockedScreen(parent);
                }
                Config.clearSyncedValues();
                return createClothConfigScreen(parent);
            });
    }

    private static Screen createClothConfigScreen(Screen parent) {
        try {
            Class<?> screenClass = Class.forName("com.ming_hai.foodstats.client.screens.FoodStatsConfigScreen");
            return (Screen) screenClass.getMethod("create", Screen.class).invoke(null, parent);
        } catch (ReflectiveOperationException | RuntimeException e) {
            FoodStatsMod.LOGGER.error("打开食物增强配置界面失败", e);
            return new RemoteServerConfigBlockedScreen(parent);
        }
    }

    private static boolean isConnectedToRemoteServer() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null && minecraft.getSingleplayerServer() == null;
    }

    public static void openFoodGuide(Player player) {
        IPlayerStats stats = FoodStatsMod.getPlayerStats(player);
        if (stats != null) {
            Minecraft.getInstance().setScreen(new FoodGuideScreen(stats));
        }
    }

    public static void logJeiInfo() {
        if (JEIIntegration.isJeiLoaded()) {
            JEIIntegration.logJeiInfo();
            FoodStatsMod.LOGGER.info("JEI 已加载，配方查看功能可用");
        } else {
            FoodStatsMod.LOGGER.info("JEI 未加载，配方查看功能不可用");
        }
    }
}
