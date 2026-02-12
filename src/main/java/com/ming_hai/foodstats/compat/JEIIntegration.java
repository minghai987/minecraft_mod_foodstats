// JEIIntegration.java - 针对1.21.1的简化版本
package com.ming_hai.foodstats.compat;

import com.ming_hai.foodstats.FoodStatsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public class JEIIntegration {
    
    public static boolean isJeiLoaded() {
        return ModList.get().isLoaded("jei");
    }
    
    public static boolean showRecipes(ItemStack stack) {
        if (!isJeiLoaded()) {
            showMessage("§e请安装 JEI 模组来查看配方");
            return false;
        }
        
        if (stack.isEmpty()) {
            showMessage("§c无法显示空物品的配方");
            return false;
        }
        
        try {
            // 尝试使用JEI API包装器
            Class<?> wrapperClass = Class.forName("com.ming_hai.foodstats.compat.jei.JEIApiWrapper");
            var showMethod = wrapperClass.getMethod("showRecipes", ItemStack.class);
            boolean result = (boolean) showMethod.invoke(null, stack);
            
            if (result) {
                return true;
            }
            
            // 如果API调用失败，尝试使用命令
            return tryJeiCommand(stack);
            
        } catch (Exception e) {
            FoodStatsMod.LOGGER.error("显示配方失败，尝试备用方案", e);
            return tryJeiCommand(stack);
        }
    }
    
    private static boolean tryJeiCommand(ItemStack stack) {
        try {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                player.connection.sendCommand("jei showRecipes " + itemId);
                FoodStatsMod.LOGGER.debug("已发送JEI命令");
                return true;
            }
        } catch (Exception e) {
            FoodStatsMod.LOGGER.debug("JEI命令失败", e);
        }
        return false;
    }
    
    private static void showMessage(String message) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal(message), false);
        }
    }
    
    public static void logJeiInfo() {
        if (isJeiLoaded()) {
            ModList.get().getModContainerById("jei").ifPresent(container -> {
                FoodStatsMod.LOGGER.info("检测到 JEI 版本: {}", container.getModInfo().getVersion());
            });
        }
    }
}