// 酒馆酒软兼容（反射识别，不强制依赖酒馆）
package com.ming_hai.foodstats.compat;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class TavernDrinkCompat {
    // 酒馆接口：有返还容器的可喝物品
    private static final String HAS_CONTAINER_CLASS =
        "com.github.ysbbbbbb.kaleidoscopetavern.item.IHasContainer";
    // 果汁桶：同样实现接口，但不计入食物增强
    private static final String JUICE_BUCKET_CLASS =
        "com.github.ysbbbbbb.kaleidoscopetavern.item.JuiceBucketItem";

    private static final Object LOCK = new Object();
    // 只解析一次：未装酒馆时也缓存失败结果，避免反复 Class.forName
    private static boolean resolved;
    private static Class<?> hasContainerClass;
    private static Class<?> juiceBucketClass;

    private TavernDrinkCompat() {
    }

    // 是否为可记入图鉴/触发增益的酒馆瓶装酒（排除果汁桶）
    public static boolean isCountableDrink(Item item) {
        if (item == null) {
            return false;
        }
        resolveClasses();
        if (hasContainerClass == null) {
            return false;
        }
        Class<?> itemClass = item.getClass();
        if (!hasContainerClass.isAssignableFrom(itemClass)) {
            return false;
        }
        return juiceBucketClass == null || !juiceBucketClass.isAssignableFrom(itemClass);
    }

    // 手册列表用：普通食物，或可计数酒馆酒
    public static boolean isGuideListedItem(Item item) {
        if (item == null) {
            return false;
        }
        ItemStack stack = new ItemStack(item);
        FoodProperties foodProperties = stack.getFoodProperties(null);
        if (foodProperties != null) {
            return true;
        }
        return isCountableDrink(item);
    }

    // 通用判定：有食物属性，或可计数酒馆酒
    public static boolean isRecordableItem(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        FoodProperties foodProperties = stack.getFoodProperties(entity);
        if (foodProperties != null) {
            return true;
        }
        return isCountableDrink(stack.getItem());
    }

    private static void resolveClasses() {
        if (resolved) {
            return;
        }
        synchronized (LOCK) {
            if (resolved) {
                return;
            }
            hasContainerClass = tryLoad(HAS_CONTAINER_CLASS);
            juiceBucketClass = tryLoad(JUICE_BUCKET_CLASS);
            resolved = true;
        }
    }

    private static Class<?> tryLoad(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            return null;
        }
    }
}
