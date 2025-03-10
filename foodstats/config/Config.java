package com.ming_hai.foodstats.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class Config {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue THRESHOLD;
    public static final ForgeConfigSpec.IntValue THRESHOLD_INCREASE;
    public static final ForgeConfigSpec.DoubleValue HEALTH_BONUS;
    public static final ForgeConfigSpec.DoubleValue ARMOR_BONUS;
    public static final ForgeConfigSpec.DoubleValue ATTACK_BONUS;
    public static final ForgeConfigSpec.ConfigValue<String> BUFF_MESSAGE;
    public static final ForgeConfigSpec.BooleanValue SHOW_MESSAGE_ABOVE_HOTBAR;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        
        builder.push("基础设置");
        THRESHOLD = builder.defineInRange("threshold", 20, 1, 1000);
        THRESHOLD_INCREASE = builder.defineInRange("threshold_increase", 0, 0, 1000);
        builder.pop();
        
        builder.push("属性加成");
        HEALTH_BONUS = builder.defineInRange("health_bonus", 1.0, 0.0, 100.0);
        ARMOR_BONUS = builder.defineInRange("armor_bonus", 1.0, 0.0, 100.0);
        ATTACK_BONUS = builder.defineInRange("attack_bonus", 1.0, 0.0, 100.0);
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
}