package com.ming_hai.foodstats.client.compat;

import com.ming_hai.foodstats.FoodStatsMod;
import com.ming_hai.foodstats.compat.jei.JEIApiWrapper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;

@OnlyIn(Dist.CLIENT)
public class ClientJEIIntegration {
    public static boolean isJeiLoaded() {
        return ModList.get().isLoaded("jei");
    }

    public static boolean showRecipes(ItemStack stack) {
        if (!isJeiLoaded() || stack.isEmpty()) return false;
        try {
            return JEIApiWrapper.showRecipes(stack);
        } catch (Throwable e) {
            FoodStatsMod.LOGGER.error("显示JEI配方失败", e);
            return false;
        }
    }
}