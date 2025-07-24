package com.ming_hai.foodstats.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import java.util.List;
import java.util.ArrayList;

public class Config {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue THRESHOLD;
    public static final ForgeConfigSpec.IntValue THRESHOLD_INCREASE;
    public static final ForgeConfigSpec.DoubleValue HEALTH_BONUS;
    public static final ForgeConfigSpec.DoubleValue ARMOR_BONUS;
    public static final ForgeConfigSpec.DoubleValue ATTACK_BONUS;
    public static final ForgeConfigSpec.ConfigValue<String> BUFF_MESSAGE;
    public static final ForgeConfigSpec.BooleanValue SHOW_MESSAGE_ABOVE_HOTBAR;
    public static final ForgeConfigSpec.IntValue MAX_BUFF_COUNT;
     public static final ForgeConfigSpec.IntValue HEALTH_MAX; // 血量上限
    public static final ForgeConfigSpec.IntValue ARMOR_MAX;  // 护甲上限
    public static final ForgeConfigSpec.IntValue ATTACK_MAX; //攻击上限
    public static final ForgeConfigSpec.DoubleValue BASE_HEALTH;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FOOD_BLACKLIST;
    public static final ForgeConfigSpec.BooleanValue START_WITH_GUIDE;


    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        
        builder.push("基础设置");
        builder.push("基础设置");
        START_WITH_GUIDE = builder
        .comment("是否在玩家首次加入时给予食物手册")
        .define("start_with_guide", true);
        THRESHOLD = builder.defineInRange("threshold", 20, 1, 1000);
        THRESHOLD_INCREASE = builder.comment("每次触发加成后阈值增加的值（0表示不增加）").defineInRange("threshold_increase", 0, 0, 1000);
        FOOD_BLACKLIST = builder
        .comment("食物黑名单（物品ID列表）")
        .defineList("food_blacklist", new ArrayList<>(), entry -> entry instanceof String);
        builder.pop();
        
        builder.push("属性加成");
        HEALTH_BONUS = builder.defineInRange("health_bonus", 1.0, 0.0, 100.0);
        ARMOR_BONUS = builder.defineInRange("armor_bonus", 1.0, 0.0, 100.0);
        ATTACK_BONUS = builder.defineInRange("attack_bonus", 1.0, 0.0, 100.0);

        // 添加三个独立的上限配置
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

        SPEC = builder.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "foodstats-config.toml");
    }
    // 在Config.java中添加同步方法
public static void sync() {
    SPEC.save();
    ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "foodstats-config.toml");
}
}