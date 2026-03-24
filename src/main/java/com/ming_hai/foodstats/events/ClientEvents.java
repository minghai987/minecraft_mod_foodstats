package com.ming_hai.foodstats.events;

import com.ming_hai.foodstats.FoodStatsMod;
import com.ming_hai.foodstats.capability.IPlayerStats;
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
import java.util.List;
import com.ming_hai.foodstats.config.Config;

// 修复：使用新的事件总线注册方式
@EventBusSubscriber(modid = FoodStatsMod.MODID, value = Dist.CLIENT)
public class ClientEvents {
    
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        
        // 检查食物属性 - 使用新的API，需要ItemStack和LivingEntity参数
        FoodProperties foodProperties = stack.getFoodProperties(event.getEntity());
        if (foodProperties == null) return;
        if (event.getEntity() == null || !event.getEntity().level().isClientSide()) return;
        
        ResourceLocation foodId = BuiltInRegistries.ITEM.getKey(item);
        if (foodId == null) return;

        // 检查黑名单
        List<String> blacklist = Config.FOOD_BLACKLIST.get();
        if (blacklist.contains(foodId.toString())) {
            event.getToolTip().add(Component.literal("该食物不足以提升你的力量").withStyle(ChatFormatting.DARK_GRAY));
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