package com.ming_hai.foodstats.config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Locale;

public class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue THRESHOLD;
    public static final ModConfigSpec.IntValue THRESHOLD_INCREASE;
    public static final ModConfigSpec.EnumValue<BonusMode> BONUS_MODE;
    public static final ModConfigSpec.IntValue UNIQUE_FOODS_PER_BONUS;
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
    public static final ModConfigSpec.ConfigValue<List<String>> FOOD_WHITELIST;
    public static final ModConfigSpec.ConfigValue<List<String>> FOOD_BLACKLIST;
    public static final ModConfigSpec.BooleanValue START_WITH_GUIDE;
    private static volatile Set<String> foodWhitelistCache = Set.of();
    private static volatile Set<String> foodBlacklistCache = Set.of();
    private static volatile SyncedValues syncedValues = null;
    private static volatile long revision = 0L;

    public enum BonusMode {
        SATURATION_THRESHOLD("按饱食度累计"),
        EACH_UNIQUE_FOOD("每种新食物一次"),
        UNIQUE_FOOD_COUNT_THRESHOLD("每若干种新食物一次");

        private final String displayName;

        BonusMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        
        builder.push("基础设置");
        START_WITH_GUIDE = builder
            .comment("是否在玩家首次加入时给予食物手册")
            .define("start_with_guide", true);
        THRESHOLD = builder.defineInRange("threshold", 20, 1, 1000);
        THRESHOLD_INCREASE = builder.comment("每次触发加成后阈值增加的值（0表示不增加）").defineInRange("threshold_increase", 0, 0, 1000);
        BONUS_MODE = builder
            .comment("增益触发方式：SATURATION_THRESHOLD=按饱食度累计，EACH_UNIQUE_FOOD=每种新食物一次，UNIQUE_FOOD_COUNT_THRESHOLD=每若干种新食物一次")
            .defineEnum("bonus_mode", BonusMode.SATURATION_THRESHOLD);
        UNIQUE_FOODS_PER_BONUS = builder
            .comment("累计多少种新的允许食物触发一次增益，仅在 UNIQUE_FOOD_COUNT_THRESHOLD 模式生效")
            .defineInRange("unique_foods_per_bonus", 10, 1, 1000);
        FOOD_WHITELIST = builder
            .comment("食物白名单（物品ID列表）。为空时不启用白名单；非空时只有白名单内食物可触发。")
            .define("food_whitelist", new ArrayList<String>());
        FOOD_BLACKLIST = builder
            .comment("食物黑名单（物品ID列表）。黑名单优先级高于白名单。")
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

        SPEC = builder.build();
    }

    public static void register() {
        // 配置注册现在在 FoodStatsMod 构造函数中处理
    }
    
    private record SyncedValues(
        boolean startWithGuide,
        int threshold,
        int thresholdIncrease,
        BonusMode bonusMode,
        int uniqueFoodsPerBonus,
        double healthBonus,
        double armorBonus,
        double attackBonus,
        int maxBuffCount,
        int healthMax,
        int armorMax,
        int attackMax,
        double baseHealth,
        String buffMessage,
        boolean showMessageAboveHotbar,
        Set<String> whitelist,
        Set<String> blacklist
    ) {
    }

    public static BonusMode getBonusMode() {
        SyncedValues synced = syncedValues;
        return synced != null ? synced.bonusMode() : BONUS_MODE.get();
    }

    public static boolean shouldStartWithGuide() {
        SyncedValues synced = syncedValues;
        return synced != null ? synced.startWithGuide() : START_WITH_GUIDE.get();
    }

    public static int getThreshold() {
        SyncedValues synced = syncedValues;
        return synced != null ? synced.threshold() : THRESHOLD.get();
    }

    public static int getThresholdIncrease() {
        SyncedValues synced = syncedValues;
        return synced != null ? synced.thresholdIncrease() : THRESHOLD_INCREASE.get();
    }

    public static int getUniqueFoodsPerBonus() {
        SyncedValues synced = syncedValues;
        return synced != null ? synced.uniqueFoodsPerBonus() : UNIQUE_FOODS_PER_BONUS.get();
    }

    public static double getHealthBonusValue() {
        SyncedValues synced = syncedValues;
        return synced != null ? synced.healthBonus() : HEALTH_BONUS.get();
    }

    public static double getArmorBonusValue() {
        SyncedValues synced = syncedValues;
        return synced != null ? synced.armorBonus() : ARMOR_BONUS.get();
    }

    public static double getAttackBonusValue() {
        SyncedValues synced = syncedValues;
        return synced != null ? synced.attackBonus() : ATTACK_BONUS.get();
    }

    public static int getMaxBuffCount() {
        SyncedValues synced = syncedValues;
        return synced != null ? synced.maxBuffCount() : MAX_BUFF_COUNT.get();
    }

    public static int getHealthMax() {
        SyncedValues synced = syncedValues;
        return synced != null ? synced.healthMax() : HEALTH_MAX.get();
    }

    public static int getArmorMax() {
        SyncedValues synced = syncedValues;
        return synced != null ? synced.armorMax() : ARMOR_MAX.get();
    }

    public static int getAttackMax() {
        SyncedValues synced = syncedValues;
        return synced != null ? synced.attackMax() : ATTACK_MAX.get();
    }

    public static double getBaseHealth() {
        SyncedValues synced = syncedValues;
        return synced != null ? synced.baseHealth() : BASE_HEALTH.get();
    }

    public static String getBuffMessage() {
        SyncedValues synced = syncedValues;
        return synced != null ? synced.buffMessage() : BUFF_MESSAGE.get();
    }

    public static boolean shouldShowMessageAboveHotbar() {
        SyncedValues synced = syncedValues;
        return synced != null ? synced.showMessageAboveHotbar() : SHOW_MESSAGE_ABOVE_HOTBAR.get();
    }

    public static void setBonusMode(BonusMode mode) {
        if (mode == null) {
            return;
        }
        BONUS_MODE.set(mode);
        sync();
    }

    public static boolean isFoodAllowed(String foodId) {
        if (foodId == null || foodId.isBlank()) {
            return false;
        }
        String normalized = foodId.toLowerCase(Locale.ROOT);
        SyncedValues synced = syncedValues;
        Set<String> blacklist = synced != null ? synced.blacklist() : foodBlacklistCache;
        if (blacklist.contains(normalized)) {
            return false;
        }
        Set<String> whitelist = synced != null ? synced.whitelist() : foodWhitelistCache;
        return whitelist.isEmpty() || whitelist.contains(normalized);
    }

    public static boolean isWhitelistEnabled() {
        SyncedValues synced = syncedValues;
        return !(synced != null ? synced.whitelist() : foodWhitelistCache).isEmpty();
    }

    public static void refreshCaches() {
        foodWhitelistCache = normalizeList(FOOD_WHITELIST.get());
        foodBlacklistCache = normalizeList(FOOD_BLACKLIST.get());
        revision++;
    }

    public static long getRevision() {
        return revision;
    }

    public static CompoundTag createSyncTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("StartWithGuide", START_WITH_GUIDE.get());
        tag.putInt("Threshold", THRESHOLD.get());
        tag.putInt("ThresholdIncrease", THRESHOLD_INCREASE.get());
        tag.putString("BonusMode", BONUS_MODE.get().name());
        tag.putInt("UniqueFoodsPerBonus", UNIQUE_FOODS_PER_BONUS.get());
        tag.putDouble("HealthBonus", HEALTH_BONUS.get());
        tag.putDouble("ArmorBonus", ARMOR_BONUS.get());
        tag.putDouble("AttackBonus", ATTACK_BONUS.get());
        tag.putInt("MaxBuffCount", MAX_BUFF_COUNT.get());
        tag.putInt("HealthMax", HEALTH_MAX.get());
        tag.putInt("ArmorMax", ARMOR_MAX.get());
        tag.putInt("AttackMax", ATTACK_MAX.get());
        tag.putDouble("BaseHealth", BASE_HEALTH.get());
        tag.putString("BuffMessage", BUFF_MESSAGE.get());
        tag.putBoolean("ShowMessageAboveHotbar", SHOW_MESSAGE_ABOVE_HOTBAR.get());
        tag.put("FoodWhitelist", listToTag(FOOD_WHITELIST.get()));
        tag.put("FoodBlacklist", listToTag(FOOD_BLACKLIST.get()));
        return tag;
    }

    public static void applySyncTag(CompoundTag tag) {
        syncedValues = new SyncedValues(
            tag.getBoolean("StartWithGuide"),
            tag.getInt("Threshold"),
            tag.getInt("ThresholdIncrease"),
            parseBonusMode(tag.getString("BonusMode")),
            tag.getInt("UniqueFoodsPerBonus"),
            tag.getDouble("HealthBonus"),
            tag.getDouble("ArmorBonus"),
            tag.getDouble("AttackBonus"),
            tag.getInt("MaxBuffCount"),
            tag.getInt("HealthMax"),
            tag.getInt("ArmorMax"),
            tag.getInt("AttackMax"),
            tag.getDouble("BaseHealth"),
            tag.getString("BuffMessage"),
            tag.getBoolean("ShowMessageAboveHotbar"),
            normalizeList(readStringList(tag, "FoodWhitelist")),
            normalizeList(readStringList(tag, "FoodBlacklist"))
        );
        revision++;
    }

    public static void clearSyncedValues() {
        if (syncedValues != null) {
            syncedValues = null;
            refreshCaches();
        }
    }

    private static BonusMode parseBonusMode(String value) {
        try {
            return BonusMode.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return BonusMode.SATURATION_THRESHOLD;
        }
    }

    private static ListTag listToTag(List<? extends String> values) {
        ListTag tag = new ListTag();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                tag.add(StringTag.valueOf(value.toLowerCase(Locale.ROOT)));
            }
        }
        return tag;
    }

    private static List<String> readStringList(CompoundTag tag, String key) {
        List<String> values = new ArrayList<>();
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            values.add(list.getString(i));
        }
        return values;
    }

    public static void addFoodToWhitelist(String foodId) {
        FOOD_WHITELIST.set(addToList(FOOD_WHITELIST.get(), foodId));
        sync();
    }

    public static void addFoodToBlacklist(String foodId) {
        FOOD_BLACKLIST.set(addToList(FOOD_BLACKLIST.get(), foodId));
        sync();
    }

    public static void removeFoodFromWhitelist(String foodId) {
        FOOD_WHITELIST.set(removeFromList(FOOD_WHITELIST.get(), foodId));
        sync();
    }

    public static void removeFoodFromBlacklist(String foodId) {
        FOOD_BLACKLIST.set(removeFromList(FOOD_BLACKLIST.get(), foodId));
        sync();
    }

    public static void clearWhitelist() {
        FOOD_WHITELIST.set(new ArrayList<>());
        sync();
    }

    public static void clearBlacklist() {
        FOOD_BLACKLIST.set(new ArrayList<>());
        sync();
    }

    public static void sync() {
        refreshCaches();
        SPEC.save();
    }

    private static Set<String> normalizeList(List<? extends String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                normalized.add(value.toLowerCase(Locale.ROOT));
            }
        }
        return Set.copyOf(normalized);
    }

    private static List<String> addToList(List<? extends String> current, String foodId) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String value : current) {
            if (value != null && !value.isBlank()) {
                values.add(value.toLowerCase(Locale.ROOT));
            }
        }
        if (foodId != null && !foodId.isBlank()) {
            values.add(foodId.toLowerCase(Locale.ROOT));
        }
        return new ArrayList<>(values);
    }

    private static List<String> removeFromList(List<? extends String> current, String foodId) {
        String target = foodId == null ? "" : foodId.toLowerCase(Locale.ROOT);
        List<String> values = new ArrayList<>();
        for (String value : current) {
            if (value != null && !value.isBlank() && !value.toLowerCase(Locale.ROOT).equals(target)) {
                values.add(value.toLowerCase(Locale.ROOT));
            }
        }
        return values;
    }
}