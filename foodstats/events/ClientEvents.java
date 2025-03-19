package com.ming_hai.foodstats.events;

import com.ming_hai.foodstats.FoodStatsMod;
import com.ming_hai.foodstats.capability.IPlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation; // 新增缺失的导入
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = FoodStatsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {
    
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        
        if (item.getFoodProperties() == null) return;

        // 确保玩家实体存在
        if (event.getEntity() == null) return;
        
        event.getEntity().getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
            ResourceLocation foodId = ForgeRegistries.ITEMS.getKey(item);
            boolean isEaten = stats.getEatenFoods().contains(foodId);

            // 添加自定义提示
            if (isEaten) {
                event.getToolTip().add(Component.literal("已食用").withStyle(ChatFormatting.GREEN));
            } else {
                event.getToolTip().add(
                    Component.literal("还没吃过呢~，要不尝尝看？")
                        .withStyle(ChatFormatting.BLUE)
                );
            }
        });
    }
}