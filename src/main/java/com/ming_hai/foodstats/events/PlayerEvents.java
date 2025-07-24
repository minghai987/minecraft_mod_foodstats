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
import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import java.util.function.BiConsumer;

@Mod.EventBusSubscriber(modid = FoodStatsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerEvents {
    private static final UUID HEALTH_UUID = UUID.fromString("a1b2c3d4-1234-5678-9012-abcdef123456");
    private static final UUID ARMOR_UUID = UUID.fromString("d4c3b2a1-4321-8765-2109-fedcba654321");
    private static final UUID ATTACK_UUID = UUID.fromString("12345678-abcd-efab-1234-567890abcdef");
    private static final BiConsumer<Player, IPlayerStats> CHECK_THRESHOLD = PlayerEvents::checkThresholdAndApplyBonus;

 
@SubscribeEvent
public static void onPlayerTravel(PlayerEvent.PlayerChangedDimensionEvent event) {
    Player player = event.getEntity();
    if (!player.level().isClientSide) {
        // 确保数据加载和应用加成
        player.getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
            // 只在维度变化时重新加载数据
            loadPlayerData(player);
            // 重新应用加成
            clearAllBonuses(player);
            applyPermanentBonus(player, stats.getHealthBonus(), 
                              stats.getArmorBonus(), stats.getAttackBonus());
        });
    }
}

@SubscribeEvent
public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
    Player player = event.getEntity();
    if (!player.level().isClientSide) {
        player.getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
            // 仅同步数据到客户端
            FoodStatsMod.syncStats(player, stats);
        });
    }
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
public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
    if (!event.getEntity().level().isClientSide) {
        event.getEntity().getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
            applyPermanentBonus(event.getEntity(), stats.getHealthBonus(), stats.getArmorBonus(), stats.getAttackBonus());
            FoodStatsMod.syncStats(event.getEntity(), stats);
        });
    }
}
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        player.getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
            applyPermanentBonus(player, stats.getHealthBonus(), stats.getArmorBonus(), stats.getAttackBonus()); 
            FoodStatsMod.syncStats(player, stats);
        });
    }
    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack itemStack = event.getItem();
        Item item = itemStack.getItem();

        if (item.getFoodProperties() == null) return;

        ResourceLocation foodId = ForgeRegistries.ITEMS.getKey(item);
        if (foodId == null) return;

        // 检查黑名单
        List<? extends String> blacklist = Config.FOOD_BLACKLIST.get();
        if (blacklist.contains(foodId.toString())) {
            return; // 跳过记录
        }

        player.getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
            if (!stats.getEatenFoods().contains(foodId)) {
                int nutrition = item.getFoodProperties().getNutrition();
                stats.addSaturation(nutrition);
                stats.getEatenFoods().add(foodId);
                
                // 新食物提示（固定绿色文字，物品栏上方）
                Component message = Component.literal("§a新食物已记录");
                player.displayClientMessage(message, true);
                
                CHECK_THRESHOLD.accept(player, stats);
            }
            FoodStatsMod.syncStats(player, stats);
        });
    }

private static void loadPlayerData(Player player) {
    player.getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag playerData = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        
        // 使用更可靠的加载方式
        if (playerData.contains("FoodStatsData")) {
            CompoundTag statsTag = playerData.getCompound("FoodStatsData");
            stats.loadNBT(statsTag);
            
            // 确保应用加成
            applyPermanentBonus(player, stats.getHealthBonus(), 
                              stats.getArmorBonus(), stats.getAttackBonus());
        }
        
        // 额外检查：如果数据为空则初始化
        else if (stats.getEatenFoods().isEmpty()) {
            FoodStatsMod.LOGGER.info("Initializing new player stats for {}", player.getScoreboardName());
            stats.reset();
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
public static void onPlayerFirstJoin(PlayerEvent.PlayerLoggedInEvent event) {
    Player player = event.getEntity();
    // 确保能力初始化
    player.getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag modData = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        
        if (Config.START_WITH_GUIDE.get() && !modData.getBoolean("HasReceivedFoodGuide")) {
            ItemStack guide = new ItemStack(FoodStatsMod.FOOD_GUIDE.get());
            player.addItem(guide);
            modData.putBoolean("HasReceivedFoodGuide", true);
            persistentData.put(Player.PERSISTED_NBT_TAG, modData);
        }
    });
}



@SubscribeEvent
public static void onPlayerClone(PlayerEvent.Clone event) {
    // 移除死亡判断，处理所有克隆情况
    event.getOriginal().reviveCaps();
    event.getOriginal().getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(oldStats -> {
        event.getEntity().getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(newStats -> {
            newStats.copyFrom(oldStats);
            
            // 保存到新玩家的持久数据
            CompoundTag persistentData = event.getEntity().getPersistentData();
            CompoundTag playerData = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
            
            CompoundTag statsTag = new CompoundTag();
            newStats.saveNBT(statsTag);
            playerData.put("FoodStatsData", statsTag);
            persistentData.put(Player.PERSISTED_NBT_TAG, playerData);
            
            // 重新应用加成
            applyPermanentBonus(event.getEntity(), 
                newStats.getHealthBonus(), 
                newStats.getArmorBonus(), 
                newStats.getAttackBonus());
            
            // 同步到客户端
            if (event.getEntity() instanceof ServerPlayer) {
                FoodStatsMod.syncStats(event.getEntity(), newStats);
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
        int baseThreshold = Config.THRESHOLD.get();
        int thresholdIncrease = Config.THRESHOLD_INCREASE.get();
        int healthMax = Config.HEALTH_MAX.get();
        int armorMax = Config.ARMOR_MAX.get();
        int attackMax = Config.ATTACK_MAX.get();

        int buffCount = stats.getBuffCount();
        boolean triggered = false;
        int loopCount = 0;
        final int MAX_LOOPS = 100; // 防止意外情况

        while (loopCount < MAX_LOOPS) {
            loopCount++;
            int nextThreshold = baseThreshold + buffCount * thresholdIncrease;
            int currentSaturation = stats.getCurrentSaturation();

            // 如果当前饱食度不足，跳出
            if (currentSaturation < nextThreshold) {
                break;
            }

            // 检查属性上限
            boolean healthCanAdd = (healthMax <= 0) || (stats.getHealthBonus() < healthMax);
            boolean armorCanAdd = (armorMax <= 0) || (stats.getArmorBonus() < armorMax);
            boolean attackCanAdd = (attackMax <= 0) || (stats.getAttackBonus() < attackMax);

            // 如果三个属性都达到上限，跳出
            if (!healthCanAdd && !armorCanAdd && !attackCanAdd) {
                break;
            }

            // 触发：增加buffCount
            buffCount++;
            stats.setBuffCount(buffCount);

            // 增加属性
            if (healthCanAdd) {
                stats.setHealthBonus(stats.getHealthBonus() + 1);
            }
            if (armorCanAdd) {
                stats.setArmorBonus(stats.getArmorBonus() + 1);
            }
            if (attackCanAdd) {
                stats.setAttackBonus(stats.getAttackBonus() + 1);
            }

            triggered = true;
        }

        if (triggered) {
            // 应用属性
            applyPermanentBonus(player, stats.getHealthBonus(), stats.getArmorBonus(), stats.getAttackBonus());
            // 显示消息
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
        applyPermanentBonus(player, stats.getHealthBonus(), 
                          stats.getArmorBonus(), stats.getAttackBonus());
        
        CompoundTag persistentData = player.getPersistentData();
        // 确保保存到正确的路径
        CompoundTag playerData = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        
        CompoundTag statsTag = new CompoundTag();
        stats.saveNBT(statsTag);
        playerData.put("FoodStatsData", statsTag);
        
        persistentData.put(Player.PERSISTED_NBT_TAG, playerData);
    });
}

    private static void applyPermanentBonus(Player player, int healthBonus, int armorBonus, int attackBonus) {
        // 应用配置上限
        int healthMax = Config.HEALTH_MAX.get();
        int armorMax = Config.ARMOR_MAX.get();
        int attackMax = Config.ATTACK_MAX.get();
        
        // 血量加成
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.removeModifier(HEALTH_UUID);
            
            double baseHealth = Config.BASE_HEALTH.get();
            if (baseHealth != 20.0) {
                healthAttr.setBaseValue(baseHealth);
            }
            
            // 应用上限
            double actualHealthBonus = healthBonus;
            if (healthMax > 0) {
                actualHealthBonus = Math.min(healthBonus, healthMax);
            }
            
            if (actualHealthBonus > 0) {    
                AttributeModifier healthModifier = new AttributeModifier(
                    HEALTH_UUID,
                    "foodstats_health_bonus",
                    Config.HEALTH_BONUS.get() * actualHealthBonus,
                    AttributeModifier.Operation.ADDITION
                );
                healthAttr.addPermanentModifier(healthModifier);
            }
        }

        // 护甲加成（应用上限）
        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.removeModifier(ARMOR_UUID);
            
            double actualArmorBonus = armorBonus;
            if (armorMax > 0) {
                actualArmorBonus = Math.min(armorBonus, armorMax);
            }
            
            if (actualArmorBonus > 0) {
                AttributeModifier armorModifier = new AttributeModifier(
                    ARMOR_UUID,
                    "foodstats_armor_bonus",
                    Config.ARMOR_BONUS.get() * actualArmorBonus,
                    AttributeModifier.Operation.ADDITION
                );
                armorAttr.addPermanentModifier(armorModifier);
            }
        }

        // 攻击力加成（应用上限）
        AttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.removeModifier(ATTACK_UUID);
            
            double actualAttackBonus = attackBonus;
            if (attackMax > 0) {
                actualAttackBonus = Math.min(attackBonus, attackMax);
            }
            
            if (actualAttackBonus > 0) {
                AttributeModifier attackModifier = new AttributeModifier(
                    ATTACK_UUID,
                    "foodstats_attack_bonus",
                    Config.ATTACK_BONUS.get() * actualAttackBonus,
                    AttributeModifier.Operation.ADDITION
                );
                attackAttr.addPermanentModifier(attackModifier);
            }
        }
    }
}
