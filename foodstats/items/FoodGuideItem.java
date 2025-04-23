package com.ming_hai.foodstats.items;

import com.ming_hai.foodstats.FoodStatsMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import com.ming_hai.foodstats.config.Config;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.*;


    public class FoodGuideItem extends Item {
        public FoodGuideItem() {
            super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)
            );
        }
    

    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            // 服务器端：同步数据
            player.getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
                FoodStatsMod.syncStats(player, stats);
            });
        } else {
            // 客户端：打开GUI
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                FoodStatsMod.PROXY.openFoodGuideScreen(player);
            });
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
    

    
}