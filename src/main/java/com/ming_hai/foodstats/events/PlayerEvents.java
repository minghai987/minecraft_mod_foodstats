package com.ming_hai.foodstats.events;

import com.ming_hai.foodstats.FoodStatsMod;
import com.ming_hai.foodstats.capability.IPlayerStats;
import com.ming_hai.foodstats.compat.TavernDrinkCompat;
import com.ming_hai.foodstats.config.Config;
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

public class PlayerEvents {
    private static final ResourceLocation HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(FoodStatsMod.MODID, "health_bonus");
    private static final ResourceLocation ARMOR_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(FoodStatsMod.MODID, "armor_bonus");
    private static final ResourceLocation ATTACK_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(FoodStatsMod.MODID, "attack_bonus");

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
            FoodStatsMod.syncStats(player, stats);  // 新增同步
            FoodStatsMod.syncConfig(player);
            FoodStatsMod.LOGGER.debug("玩家数据已更新");
            
            // 首次加入给予食物手册
            if (Config.shouldStartWithGuide() && !player.getPersistentData().getBoolean("HasReceivedFoodGuide")) {
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

        // 普通食物，或酒馆 IHasContainer 瓶装酒（排除果汁桶）
        FoodProperties foodProperties = itemStack.getFoodProperties(player);
        boolean tavernDrink = foodProperties == null && TavernDrinkCompat.isCountableDrink(item);
        if (foodProperties == null && !tavernDrink) {
            return;
        }

        ResourceLocation foodId = BuiltInRegistries.ITEM.getKey(item);
        if (foodId == null) return;

        if (!Config.isFoodAllowed(foodId.toString())) {
            return;
        }

        IPlayerStats stats = FoodStatsMod.getPlayerStats(player);
        if (stats != null) {
            if (!stats.getEatenFoods().contains(foodId)) {
                stats.getEatenFoods().add(foodId);
                
                // 新食物提示
                Component message = Component.literal("§a新食物已记录");
                player.displayClientMessage(message, true);
                
                // 酒馆酒无 FoodProperties，营养按 0（饱食度模式只记图鉴）
                int nutrition = foodProperties != null ? foodProperties.nutrition() : 0;
                handleNewFoodBonus(player, stats, nutrition);
                FoodStatsMod.syncStats(player, stats);      // 新增同步
            }
            FoodStatsMod.LOGGER.debug("玩家数据已更新");
        }
    }

@SubscribeEvent
public void onPlayerClone(PlayerEvent.Clone event) {
    if (event.isWasDeath()) {
        // 手动复制食物手册标记
        CompoundTag oldPersistent = event.getOriginal().getPersistentData();
        CompoundTag newPersistent = event.getEntity().getPersistentData();
        if (oldPersistent.contains("HasReceivedFoodGuide")) {
            newPersistent.putBoolean("HasReceivedFoodGuide", 
                oldPersistent.getBoolean("HasReceivedFoodGuide"));
            FoodStatsMod.LOGGER.debug("已复制食物手册标记");
        }
        // 系统已自动通过 copyOnDeath 复制其他数据
        FoodStatsMod.LOGGER.debug("玩家死亡重生，数据已由系统自动复制");
    }
}

    private void handleNewFoodBonus(Player player, IPlayerStats stats, int nutrition) {
        switch (Config.getBonusMode()) {
            case SATURATION_THRESHOLD -> {
                stats.addSaturation(nutrition);
                checkSaturationThresholdAndApplyBonus(player, stats);
            }
            case EACH_UNIQUE_FOOD -> tryApplyOneBonus(player, stats);
            case UNIQUE_FOOD_COUNT_THRESHOLD -> {
                stats.incrementUniqueFoodProgress();
                int requiredFoods = Config.getUniqueFoodsPerBonus();
                while (stats.getUniqueFoodProgress() >= requiredFoods && tryApplyOneBonus(player, stats)) {
                    stats.setUniqueFoodProgress(stats.getUniqueFoodProgress() - requiredFoods);
                }
            }
        }
    }

    private void checkSaturationThresholdAndApplyBonus(Player player, IPlayerStats stats) {
        int baseThreshold = Config.getThreshold();
        int thresholdIncrease = Config.getThresholdIncrease();
        int loopCount = 0;
        final int MAX_LOOPS = 100;

        while (loopCount < MAX_LOOPS) {
            loopCount++;
            int nextThreshold = baseThreshold + stats.getBuffCount() * thresholdIncrease;
            int currentSaturation = stats.getCurrentSaturation();

            if (currentSaturation < nextThreshold) {
                break;
            }

            if (!tryApplyOneBonus(player, stats)) {
                break;
            }
        }
    }

    private boolean tryApplyOneBonus(Player player, IPlayerStats stats) {
        int maxBuff = Config.getMaxBuffCount();
        if (maxBuff > 0 && stats.getBuffCount() >= maxBuff) {
            return false;
        }

        boolean healthCanAdd = Config.getHealthBonusValue() > 0 && (Config.getHealthMax() <= 0 || stats.getHealthBonus() < Config.getHealthMax());
        boolean armorCanAdd = Config.getArmorBonusValue() > 0 && (Config.getArmorMax() <= 0 || stats.getArmorBonus() < Config.getArmorMax());
        boolean attackCanAdd = Config.getAttackBonusValue() > 0 && (Config.getAttackMax() <= 0 || stats.getAttackBonus() < Config.getAttackMax());

        if (!healthCanAdd && !armorCanAdd && !attackCanAdd) {
            return false;
        }

        stats.setBuffCount(stats.getBuffCount() + 1);
        if (healthCanAdd) {
            stats.setHealthBonus(stats.getHealthBonus() + 1);
        }
        if (armorCanAdd) {
            stats.setArmorBonus(stats.getArmorBonus() + 1);
        }
        if (attackCanAdd) {
            stats.setAttackBonus(stats.getAttackBonus() + 1);
        }

        applyPermanentBonus(player, stats.getHealthBonus(), stats.getArmorBonus(), stats.getAttackBonus());
        Component buffMessage = Component.literal(Config.getBuffMessage());
        if (Config.shouldShowMessageAboveHotbar()) {
            player.displayClientMessage(buffMessage, true);
        } else {
            player.sendSystemMessage(buffMessage);
        }
        return true;
    }

    public static void applyPermanentBonus(Player player, int healthBonus, int armorBonus, int attackBonus) {
        int healthMax = Config.getHealthMax();
        int armorMax = Config.getArmorMax();
        int attackMax = Config.getAttackMax();
        
        // 保存血量比例（避免绝对值失效）
        float oldMaxHealth = player.getMaxHealth();
        float healthRatio = (oldMaxHealth > 0) ? player.getHealth() / oldMaxHealth : 1.0f;
        // 血量加成
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.removeModifier(HEALTH_MODIFIER_ID);
            
            double baseHealth = Config.getBaseHealth();
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
                    Config.getHealthBonusValue() * actualHealthBonus,
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
                    Config.getArmorBonusValue() * actualArmorBonus,
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
                    Config.getAttackBonusValue() * actualAttackBonus,
                    AttributeModifier.Operation.ADD_VALUE
                );
                attackAttr.addTransientModifier(attackModifier);
            }
        }

        // 按比例恢复血量
    float newMaxHealth = player.getMaxHealth();
    float newHealth = healthRatio * newMaxHealth;
    if (newHealth > newMaxHealth) newHealth = newMaxHealth;
    if (newHealth < 0) newHealth = 0;
    player.setHealth(newHealth);
    
    FoodStatsMod.LOGGER.debug("应用永久加成: 血量比例={}, 新血量={}/{}", 
        healthRatio, newHealth, newMaxHealth);
    }
}