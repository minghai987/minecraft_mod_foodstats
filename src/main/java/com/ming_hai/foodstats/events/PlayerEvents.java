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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.food.FoodProperties;

import java.util.UUID;
import java.util.List;
import java.util.function.BiConsumer;

public class PlayerEvents {
    private static final ResourceLocation HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(FoodStatsMod.MODID, "health_bonus");
    private static final ResourceLocation ARMOR_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(FoodStatsMod.MODID, "armor_bonus");
    private static final ResourceLocation ATTACK_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(FoodStatsMod.MODID, "attack_bonus");
    private final BiConsumer<Player, IPlayerStats> CHECK_THRESHOLD = this::checkThresholdAndApplyBonus;

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            IPlayerStats stats = FoodStatsMod.getPlayerStats(event.getEntity());
            if (stats != null) {
                applyPermanentBonus(event.getEntity(), stats.getHealthBonus(), stats.getArmorBonus(), stats.getAttackBonus());
                FoodStatsMod.syncStats(event.getEntity(), stats);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        IPlayerStats stats = FoodStatsMod.getPlayerStats(player);
        if (stats != null) {
            applyPermanentBonus(player, stats.getHealthBonus(), stats.getArmorBonus(), stats.getAttackBonus()); 
            FoodStatsMod.LOGGER.debug("玩家数据已更新");
            
            // 首次加入给予食物手册
            if (Config.START_WITH_GUIDE.get() && !player.getPersistentData().getBoolean("HasReceivedFoodGuide")) {
                ItemStack guide = new ItemStack(FoodStatsMod.FOOD_GUIDE.get());
                player.addItem(guide);
                player.getPersistentData().putBoolean("HasReceivedFoodGuide", true);
            }
        }
    }

    @SubscribeEvent
    public void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack itemStack = event.getItem();
        Item item = itemStack.getItem();

        // 检查食物属性 - 使用新的API
        FoodProperties foodProperties = itemStack.getFoodProperties(player);
        if (foodProperties == null) return;

        ResourceLocation foodId = BuiltInRegistries.ITEM.getKey(item);
        if (foodId == null) return;

        // 检查黑名单
        List<String> blacklist = Config.FOOD_BLACKLIST.get();
        if (blacklist.contains(foodId.toString())) {
            return;
        }

        IPlayerStats stats = FoodStatsMod.getPlayerStats(player);
        if (stats != null) {
            if (!stats.getEatenFoods().contains(foodId)) {
                // 使用食物的营养值作为饱食度
                int nutrition = foodProperties != null ? foodProperties.nutrition() : 1;
                stats.addSaturation(nutrition);
                stats.getEatenFoods().add(foodId);
                
                // 新食物提示
                Component message = Component.literal("§a新食物已记录");
                player.displayClientMessage(message, true);
                
                CHECK_THRESHOLD.accept(player, stats);
            }
            FoodStatsMod.LOGGER.debug("玩家数据已更新");
        }
    }

@SubscribeEvent
public void onPlayerClone(PlayerEvent.Clone event) {
    // 确保是从死亡克隆
    if (event.isWasDeath()) {
        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();
        
        // 获取统计数据
        IPlayerStats oldStats = FoodStatsMod.getPlayerStats(original);
        IPlayerStats newStats = FoodStatsMod.getPlayerStats(newPlayer);
        
        if (oldStats != null && newStats != null) {
            // 复制所有数据
            newStats.copyFrom(oldStats);
            
            // 重新应用加成
            applyPermanentBonus(newPlayer, 
                newStats.getHealthBonus(), 
                newStats.getArmorBonus(), 
                newStats.getAttackBonus());
            
            // 同步到客户端
            FoodStatsMod.syncStats(newPlayer, newStats);
            
            FoodStatsMod.LOGGER.info("玩家数据已从死亡实体复制到新实体");
        }
    }
}

    private void checkThresholdAndApplyBonus(Player player, IPlayerStats stats) {
        int baseThreshold = Config.THRESHOLD.get();
        int thresholdIncrease = Config.THRESHOLD_INCREASE.get();
        int healthMax = Config.HEALTH_MAX.get();
        int armorMax = Config.ARMOR_MAX.get();
        int attackMax = Config.ATTACK_MAX.get();

        int buffCount = stats.getBuffCount();
        boolean triggered = false;
        int loopCount = 0;
        final int MAX_LOOPS = 100;

        while (loopCount < MAX_LOOPS) {
            loopCount++;
            int nextThreshold = baseThreshold + buffCount * thresholdIncrease;
            int currentSaturation = stats.getCurrentSaturation();

            if (currentSaturation < nextThreshold) {
                break;
            }

            boolean healthCanAdd = (healthMax <= 0) || (stats.getHealthBonus() < healthMax);
            boolean armorCanAdd = (armorMax <= 0) || (stats.getArmorBonus() < armorMax);
            boolean attackCanAdd = (attackMax <= 0) || (stats.getAttackBonus() < attackMax);

            if (!healthCanAdd && !armorCanAdd && !attackCanAdd) {
                break;
            }

            buffCount++;
            stats.setBuffCount(buffCount);

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
            applyPermanentBonus(player, stats.getHealthBonus(), stats.getArmorBonus(), stats.getAttackBonus());
            Component buffMessage = Component.literal(Config.BUFF_MESSAGE.get());
            if (Config.SHOW_MESSAGE_ABOVE_HOTBAR.get()) {
                player.displayClientMessage(buffMessage, true);
            } else {
                player.sendSystemMessage(buffMessage);
            }
        }
    }

    private static void applyPermanentBonus(Player player, int healthBonus, int armorBonus, int attackBonus) {
        int healthMax = Config.HEALTH_MAX.get();
        int armorMax = Config.ARMOR_MAX.get();
        int attackMax = Config.ATTACK_MAX.get();
        
        // 血量加成
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.removeModifier(HEALTH_MODIFIER_ID);
            
            double baseHealth = Config.BASE_HEALTH.get();
            if (baseHealth != 20.0) {
                healthAttr.setBaseValue(baseHealth);
            }
            
            double actualHealthBonus = healthBonus;
            if (healthMax > 0) {
                actualHealthBonus = Math.min(healthBonus, healthMax);
            }
            
            if (actualHealthBonus > 0) {    
                AttributeModifier healthModifier = new AttributeModifier(
                    HEALTH_MODIFIER_ID,
                    Config.HEALTH_BONUS.get() * actualHealthBonus,
                    AttributeModifier.Operation.ADD_VALUE
                );
                healthAttr.addTransientModifier(healthModifier);
            }
        }

        // 护甲加成
        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.removeModifier(ARMOR_MODIFIER_ID);
            
            double actualArmorBonus = armorBonus;
            if (armorMax > 0) {
                actualArmorBonus = Math.min(armorBonus, armorMax);
            }
            
            if (actualArmorBonus > 0) {
                AttributeModifier armorModifier = new AttributeModifier(
                    ARMOR_MODIFIER_ID,
                    Config.ARMOR_BONUS.get() * actualArmorBonus,
                    AttributeModifier.Operation.ADD_VALUE
                );
                armorAttr.addTransientModifier(armorModifier);
            }
        }

        // 攻击力加成
        AttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.removeModifier(ATTACK_MODIFIER_ID);
            
            double actualAttackBonus = attackBonus;
            if (attackMax > 0) {
                actualAttackBonus = Math.min(attackBonus, attackMax);
            }
            
            if (actualAttackBonus > 0) {
                AttributeModifier attackModifier = new AttributeModifier(
                    ATTACK_MODIFIER_ID,
                    Config.ATTACK_BONUS.get() * actualAttackBonus,
                    AttributeModifier.Operation.ADD_VALUE
                );
                attackAttr.addTransientModifier(attackModifier);
            }
        }
    }
}