package com.ming_hai.foodstats.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;
import java.util.ArrayList;

public class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue THRESHOLD;
    public static final ModConfigSpec.IntValue THRESHOLD_INCREASE;
    public static final ModConfigSpec.DoubleValue HEALTH_BONUS;
    public static final ModConfigSpec.DoubleValue ARMOR_BONUS;
    public static final ModConfigSpec.DoubleValue ATTACK_BONUS;
    public static final ModConfigSpec.ConfigValue<String> BUFF_MESSAGE;
    public static final ModConfigSpec.BooleanValue SHOW_MESSAGE_ABOVE_HOTBAR;
    public static final ModConfigSpec.IntValue MAX_BUFF_COUNT;
    public static final ModConfigSpec.IntValue HEALTH_MAX;
    public static final ModConfigSpec.IntValue ARMOR_MAX;
    public static final ModConfigSpec.IntValue ATTACK_MAX;
    public static final ModConfigSpec.DoubleValue BASE_HEALTH;
    public static final ModConfigSpec.ConfigValue<List<String>> FOOD_BLACKLIST;
    public static final ModConfigSpec.BooleanValue START_WITH_GUIDE;
    // 方块食物配置：记录物品ID、每次食用的营养值、是否可作为食物（用于图鉴）
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_FOOD_ITEMS;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> BLOCK_FOOD_NUTRITION;


    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        
        builder.push("基础设置");
        START_WITH_GUIDE = builder
            .comment("是否在玩家首次加入时给予食物手册")
            .define("start_with_guide", true);
        THRESHOLD = builder.defineInRange("threshold", 20, 1, 1000);
        THRESHOLD_INCREASE = builder.comment("每次触发加成后阈值增加的值（0表示不增加）").defineInRange("threshold_increase", 0, 0, 1000);
        FOOD_BLACKLIST = builder
            .comment("食物黑名单（物品ID列表）")
            .define("food_blacklist", new ArrayList<String>());
        builder.pop();
        
        builder.push("属性加成");
        HEALTH_BONUS = builder.defineInRange("health_bonus", 1.0, 0.0, 100.0);
        ARMOR_BONUS = builder.defineInRange("armor_bonus", 1.0, 0.0, 100.0);
        ATTACK_BONUS = builder.defineInRange("attack_bonus", 1.0, 0.0, 100.0);

        HEALTH_MAX = builder
            .comment("血量加成上限（次数）（0表示无上限）")
            .defineInRange("health_max", 0, 0, 1000);
        ARMOR_MAX = builder
            .comment("护甲加成上限（次数）（0表示无上限）")
            .defineInRange("armor_max", 0, 0, 1000);
        ATTACK_MAX = builder
            .comment("攻击加成上限（次数）（0表示无上限）")
            .defineInRange("attack_max", 0, 0, 1000);
            
        BASE_HEALTH = builder
            .comment("初始血量（未获得加成时的基础血量）")
            .defineInRange("base_health", 20.0, 1.0, 100.0);
        
        MAX_BUFF_COUNT = builder
            .comment("数值提升上限（0表示无上限）")
            .defineInRange("max_buff_count", 0, 0, 1000);
        builder.pop();

        builder.push("消息设置");
        BUFF_MESSAGE = builder
            .comment("触发buff时显示的消息（支持颜色代码）")
            .define("buff_message", "§a感觉你的力量恢复了一些");
        SHOW_MESSAGE_ABOVE_HOTBAR = builder
            .comment("是否在物品栏上方显示提示")
            .define("show_message_above_hotbar", true);
        builder.pop();


         builder.push("方块食物设置");
        BLOCK_FOOD_ITEMS = builder
            .comment("方块食物列表（物品ID），例如 minecraft:cake")
            .defineList("block_food_items", List.of("minecraft:cake"), o -> o instanceof String);
        BLOCK_FOOD_NUTRITION = builder
            .comment("对应上方列表的营养值，顺序必须一致")
            .defineList("block_food_nutrition", List.of(2), o -> o instanceof Integer);
        builder.pop();

        SPEC = builder.build();
    }

    public static void register() {
        // 配置注册现在在 FoodStatsMod 构造函数中处理
    }
    
    public static void sync() {
        SPEC.save();


    }
}