// FoodGuideItem.java
package com.ming_hai.foodstats.items;

import com.ming_hai.foodstats.FoodStatsMod;
import com.ming_hai.foodstats.capability.IPlayerStats;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

public class FoodGuideItem extends Item {
    public FoodGuideItem() {
        super(new Item.Properties()
            .stacksTo(1)
            .rarity(Rarity.UNCOMMON)
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            // 客户端直接打开界面
            IPlayerStats stats = FoodStatsMod.getPlayerStats(player);
            if (stats != null) {
                Minecraft.getInstance().setScreen(
                    new com.ming_hai.foodstats.client.screens.FoodGuideScreen(stats)
                );
            }
        } else {
            // 服务器端，暂时不需要做任何事情
            FoodStatsMod.LOGGER.debug("服务器端使用食物手册");
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}