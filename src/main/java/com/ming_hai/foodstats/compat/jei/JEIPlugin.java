// JEIPlugin.java - 针对JEI 19.x的版本
package com.ming_hai.foodstats.compat.jei;

import com.ming_hai.foodstats.FoodStatsMod;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
    
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(FoodStatsMod.MODID, "jei_plugin");
    }
    
    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        // 设置运行时到包装器
        JEIApiWrapper.setRuntime(runtime);
        FoodStatsMod.LOGGER.info("JEI 19.x 运行时已可用 - 配方查看功能已启用");
    }
    
    @Override
    public void onRuntimeUnavailable() {
        // 清理运行时
        JEIApiWrapper.setRuntime(null);
        FoodStatsMod.LOGGER.info("JEI 运行时不可用");
    }
}