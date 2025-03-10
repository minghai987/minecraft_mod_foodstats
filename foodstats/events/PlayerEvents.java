package com.ming_hai.foodstats.events;

import com.ming_hai.foodstats.FoodStatsMod;
import com.ming_hai.foodstats.config.Config;
import com.ming_hai.foodstats.capability.IPlayerStats;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = FoodStatsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerEvents {
    private static final UUID HEALTH_UUID = UUID.fromString("a1b2c3d4-1234-5678-9012-abcdef123456");
    private static final UUID ARMOR_UUID = UUID.fromString("d4c3b2a1-4321-8765-2109-fedcba654321");
    private static final UUID ATTACK_UUID = UUID.fromString("12345678-abcd-efab-1234-567890abcdef");

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack itemStack = event.getItem();
        Item item = itemStack.getItem();

        if (item.getFoodProperties() == null) return;

        player.getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
            ResourceLocation foodId = ForgeRegistries.ITEMS.getKey(item);
            if (!stats.getEatenFoods().contains(foodId)) {
                int nutrition = item.getFoodProperties().getNutrition();
                stats.addSaturation(nutrition);
                stats.getEatenFoods().add(foodId);
                
                // 新食物提示（固定绿色文字，物品栏上方）
                Component message = Component.literal("§a新食物已记录");
                player.displayClientMessage(message, true);
                
                checkThresholdAndApplyBonus(player, stats);
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerSave(PlayerEvent.SaveToFile event) {
        savePlayerData(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoad(PlayerEvent.LoadFromFile event) {
        loadPlayerData(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        
        Player original = event.getOriginal();
        original.reviveCaps();
        
        LazyOptional<IPlayerStats> oldStats = original.getCapability(FoodStatsMod.PLAYER_STATS);
        LazyOptional<IPlayerStats> newStats = event.getEntity().getCapability(FoodStatsMod.PLAYER_STATS);

        oldStats.ifPresent(old -> {
            newStats.ifPresent(newS -> {
                newS.copyFrom(old);
                event.getEntity().displayClientMessage(
                    Component.literal("§a食物统计已恢复"),
                    true
                );
            });
        });
        
        original.invalidateCaps();
    }

    private static void checkThresholdAndApplyBonus(Player player, IPlayerStats stats) {
        int currentThreshold = Config.THRESHOLD.get();
        int consumed = 0;
        int buffsToApply = 0;

        while (stats.getTotalSaturation() >= currentThreshold) {
            buffsToApply++;
            stats.addSaturation(-currentThreshold);
            consumed += currentThreshold;

            if (Config.THRESHOLD_INCREASE.get() > 0) {
                currentThreshold += Config.THRESHOLD_INCREASE.get();
            } else {
                break;
            }
        }

        if (buffsToApply > 0) {
            stats.setBuffCount(stats.getBuffCount() + buffsToApply);
            applyPermanentBonus(player, stats.getBuffCount());
            
            // 从配置读取消息内容和位置
            Component buffMessage = Component.literal(Config.BUFF_MESSAGE.get());
            if (Config.SHOW_MESSAGE_ABOVE_HOTBAR.get()) {
                player.displayClientMessage(buffMessage, true);
            } else {
                player.sendSystemMessage(buffMessage);
            }
        }
    }

    private static void savePlayerData(Player player) {
        player.getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
            CompoundTag persistentData = player.getPersistentData();
            CompoundTag statsTag = new CompoundTag();
            stats.saveNBT(statsTag);
            persistentData.put("FoodStats", statsTag);
        });
    }

    private static void loadPlayerData(Player player) {
        player.getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
            CompoundTag persistentData = player.getPersistentData();
            if (persistentData.contains("FoodStats")) {
                stats.loadNBT(persistentData.getCompound("FoodStats"));
            }
        });
    }

    private static void applyPermanentBonus(Player player, int totalBuffs) {
        // 血量加成
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.removeModifier(HEALTH_UUID);
            AttributeModifier healthModifier = new AttributeModifier(
                HEALTH_UUID,
                "foodstats_health_bonus",
                Config.HEALTH_BONUS.get() * totalBuffs,
                AttributeModifier.Operation.ADDITION
            );
            healthAttr.addPermanentModifier(healthModifier);
        }

        // 护甲加成
        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.removeModifier(ARMOR_UUID);
            AttributeModifier armorModifier = new AttributeModifier(
                ARMOR_UUID,
                "foodstats_armor_bonus",
                Config.ARMOR_BONUS.get() * totalBuffs,
                AttributeModifier.Operation.ADDITION
            );
            armorAttr.addPermanentModifier(armorModifier);
        }

        // 攻击力加成
        AttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.removeModifier(ATTACK_UUID);
            AttributeModifier attackModifier = new AttributeModifier(
                ATTACK_UUID,
                "foodstats_attack_bonus",
                Config.ATTACK_BONUS.get() * totalBuffs,
                AttributeModifier.Operation.ADDITION
            );
            attackAttr.addPermanentModifier(attackModifier);
        }

        // 强制更新当前血量
        float newHealth = player.getHealth() + (Config.HEALTH_BONUS.get().floatValue() * totalBuffs);
        player.setHealth(Math.min(newHealth, player.getMaxHealth()));
    }
}