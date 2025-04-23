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
import net.minecraft.resources.ResourceLocation;

@Mod.EventBusSubscriber(modid = FoodStatsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerEvents {
    private static final UUID HEALTH_UUID = UUID.fromString("a1b2c3d4-1234-5678-9012-abcdef123456");
    private static final UUID ARMOR_UUID = UUID.fromString("d4c3b2a1-4321-8765-2109-fedcba654321");
    private static final UUID ATTACK_UUID = UUID.fromString("12345678-abcd-efab-1234-567890abcdef");

    @SubscribeEvent
public static void onPlayerTravel(PlayerEvent.PlayerChangedDimensionEvent event) {
    Player player = event.getEntity();
    player.getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
        // 立即保存当前维度数据
        savePlayerData(player);
        // 应用属性加成前移除旧加成
        clearAllBonuses(player);
        // 重新应用加成
        applyPermanentBonus(player, stats.getBuffCount());
        // 延迟同步确保实体完全加载
        player.getServer().execute(() -> {
            FoodStatsMod.syncStats(player, stats);
        });
    });
}
private static void clearAllBonuses(Player player) {
    AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
    if (healthAttr != null) healthAttr.removeModifier(HEALTH_UUID);
    
    AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
    if (armorAttr != null) armorAttr.removeModifier(ARMOR_UUID);
    
    AttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (attackAttr != null) attackAttr.removeModifier(ATTACK_UUID);
}
    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        player.getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
            applyPermanentBonus(player, stats.getBuffCount());
            FoodStatsMod.syncStats(player, stats);
        });
    }
    @SubscribeEvent
public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
    if (!event.getEntity().level().isClientSide) {
        event.getEntity().getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
            applyPermanentBonus(event.getEntity(), stats.getBuffCount());
            FoodStatsMod.syncStats(event.getEntity(), stats);
        });
    }
}
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        player.getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
            applyPermanentBonus(player, stats.getBuffCount());
            FoodStatsMod.syncStats(player, stats);
        });
    }
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
            FoodStatsMod.syncStats(player, stats);
        });
    }
    private static void loadPlayerData(Player player) {
        player.getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
            CompoundTag persistentData = player.getPersistentData();
            if (persistentData.contains("FoodStatsData")) {
                stats.loadNBT(persistentData.getCompound("FoodStatsData"));
                applyPermanentBonus(player, stats.getBuffCount());
                
                // 延迟1tick确保玩家完全加载
                if (!player.level().isClientSide) {
                    player.getServer().execute(() -> {
                        if (player.isAlive()) {
                            FoodStatsMod.syncStats(player, stats);
                        }
                    });
                }
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
        // 无论是否死亡都复制数据
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(oldStats -> {
            event.getEntity().getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(newStats -> {
                newStats.copyFrom(oldStats);
                // 立即保存到新玩家的持久数据
                CompoundTag persistentData = event.getEntity().getPersistentData();
                CompoundTag statsTag = new CompoundTag();
                newStats.saveNBT(statsTag);
                persistentData.put("FoodStatsData", statsTag);
                // 延迟同步确保实体有效
                if (!event.getEntity().level().isClientSide) {
                    event.getEntity().getServer().execute(() -> {
                        FoodStatsMod.syncStats(event.getEntity(), newStats);
                    });
                }
            });
        });
        event.getOriginal().invalidateCaps();
    }
    @SubscribeEvent
public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    savePlayerData(event.getEntity());
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
            // 确保应用最新属性后再保存
            applyPermanentBonus(player, stats.getBuffCount());
            CompoundTag persistentData = player.getPersistentData();
            CompoundTag statsTag = new CompoundTag();
            stats.saveNBT(statsTag);
            persistentData.put("FoodStatsData", statsTag); // 数据会自动随玩家保存
        });
    }

    private static void applyPermanentBonus(Player player, int totalBuffs) {
        // 血量加成
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.removeModifier(HEALTH_UUID);
            if (totalBuffs > 0) {    
                AttributeModifier healthModifier = new AttributeModifier(
                    HEALTH_UUID,
                    "foodstats_health_bonus",
                    Config.HEALTH_BONUS.get() * totalBuffs,
                    AttributeModifier.Operation.ADDITION
            );
            healthAttr.addPermanentModifier(healthModifier);
        }
        }

        // 护甲加成
        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.removeModifier(ARMOR_UUID);
            if (totalBuffs > 0) {
                AttributeModifier armorModifier = new AttributeModifier(
                ARMOR_UUID,
                "foodstats_armor_bonus",
                Config.ARMOR_BONUS.get() * totalBuffs,
                AttributeModifier.Operation.ADDITION
            );
            armorAttr.addPermanentModifier(armorModifier);
        }
        }

        // 攻击力加成
        AttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.removeModifier(ATTACK_UUID);
            if (totalBuffs > 0) {
            AttributeModifier attackModifier = new AttributeModifier(
                ATTACK_UUID,
                "foodstats_attack_bonus",
                Config.ATTACK_BONUS.get() * totalBuffs,
                AttributeModifier.Operation.ADDITION
            );
            attackAttr.addPermanentModifier(attackModifier);
        }
        }

        // 强制更新当前血量
        float newHealth = player.getHealth() + (Config.HEALTH_BONUS.get().floatValue() * totalBuffs);
        player.setHealth(Math.min(newHealth, player.getMaxHealth()));
    }
}