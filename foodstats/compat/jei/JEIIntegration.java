// JEIIntegration.java 确保正确初始化
package com.ming_hai.foodstats.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import com.ming_hai.foodstats.FoodStatsMod;
import net.minecraft.resources.ResourceLocation;
import mezz.jei.api.registration.IRecipeRegistration;


@JeiPlugin
public class JEIIntegration implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(FoodStatsMod.MODID, "jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        FoodStatsMod.setJeiRuntime(jeiRuntime);
        FoodStatsMod.LOGGER.info("JEI Runtime initialized");
    }
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 若需要注册配方可以在此实现
    }
}