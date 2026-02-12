// JEIApiWrapper.java - 修复版本
package com.ming_hai.foodstats.compat.jei;

import com.ming_hai.foodstats.FoodStatsMod;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;

public class JEIApiWrapper {
    private static IJeiRuntime jeiRuntime;
    
    public static void setRuntime(IJeiRuntime runtime) {
        jeiRuntime = runtime;
        FoodStatsMod.LOGGER.info("JEI 运行时已设置");
    }
    
    public static IJeiRuntime getRuntime() {
        return jeiRuntime;
    }
    
    public static boolean showRecipes(ItemStack stack) {
        if (jeiRuntime == null || stack.isEmpty()) {
            return false;
        }
        
        try {
            // 关键修复：使用 OUTPUT 而不是 INPUT
            // INPUT 显示"这个物品能用来做什么"（用途）
            // OUTPUT 显示"如何制作这个物品"（合成配方）
            var focus = jeiRuntime.getJeiHelpers().getFocusFactory()
                .createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack);
            
            if (focus != null) {
                jeiRuntime.getRecipesGui().show(focus);
                FoodStatsMod.LOGGER.debug("成功显示 {} 的合成配方", stack.getItem());
                return true;
            }
        } catch (Exception e) {
            FoodStatsMod.LOGGER.error("JEI API 调用失败", e);
        }
        
        return false;
    }
    
    public static boolean isAvailable() {
        return jeiRuntime != null;
    }
}