package com.ming_hai.foodstats.events;

import com.ming_hai.foodstats.FoodStatsMod;
import com.ming_hai.foodstats.capability.IPlayerStats;
import com.ming_hai.foodstats.compat.TavernDrinkCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.food.FoodProperties;
import com.ming_hai.foodstats.config.Config;

// 修复：使用新的事件总线注册方式
@EventBusSubscriber(modid = FoodStatsMod.MODID, value = Dist.CLIENT)
public class ClientEvents {
    
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        
        // 普通食物，或可计数酒馆酒，才显示食用状态
        FoodProperties foodProperties = stack.getFoodProperties(event.getEntity());
        boolean recordable = foodProperties != null || TavernDrinkCompat.isCountableDrink(item);
        if (!recordable) return;
        if (event.getEntity() == null || !event.getEntity().level().isClientSide()) return;
        
        ResourceLocation foodId = BuiltInRegistries.ITEM.getKey(item);
        if (foodId == null) return;

        if (!Config.isFoodAllowed(foodId.toString())) {
            String message = Config.isWhitelistEnabled() ? "该食物未加入食物白名单" : "该食物已被食物黑名单排除";
            event.getToolTip().add(Component.literal(message).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        
        if (event.getEntity().level().isClientSide()) {
            IPlayerStats stats = FoodStatsMod.getPlayerStats(event.getEntity());
            if (stats != null) {
                boolean isEaten = stats.getEatenFoods().contains(foodId);
                if (isEaten) {
                    event.getToolTip().add(Component.literal("已食用").withStyle(ChatFormatting.GREEN));
                } else {
                    event.getToolTip().add(
                        Component.literal("还没吃过呢~，要不尝尝看？")
                            .withStyle(ChatFormatting.BLUE)
                    );
                }
            }
        }
    }
}
