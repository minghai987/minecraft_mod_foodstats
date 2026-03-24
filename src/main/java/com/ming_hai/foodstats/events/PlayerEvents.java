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
import net.minecraft.world.level.block.CakeBlock;

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
            FoodStatsMod.syncStats(player, stats);  // 新增同步
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

    private void checkThresholdAndApplyBonus(Player player, IPlayerStats stats) {
        int baseThreshold = Config.THRESHOLD.get();
        int thresholdIncrease = Config.THRESHOLD_INCREASE.get();
        int healthMax = Config.HEALTH_MAX.get();
        int armorMax = Config.ARMOR_MAX.get();
        int attackMax = Config.ATTACK_MAX.get();
        int maxBuff = Config.MAX_BUFF_COUNT.get();  // 获取总加成次数上限（0表示无上限）

        int buffCount = stats.getBuffCount();
        boolean triggered = false;
        int loopCount = 0;
        final int MAX_LOOPS = 100;

        while (loopCount < MAX_LOOPS) {
            loopCount++;
            if (maxBuff > 0 && buffCount >= maxBuff) {
            break;
        }

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
        
        // 保存血量比例（避免绝对值失效）
        float oldMaxHealth = player.getMaxHealth();
        float healthRatio = (oldMaxHealth > 0) ? player.getHealth() / oldMaxHealth : 1.0f;
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

        // 按比例恢复血量
    float newMaxHealth = player.getMaxHealth();
    float newHealth = healthRatio * newMaxHealth;
    if (newHealth > newMaxHealth) newHealth = newMaxHealth;
    if (newHealth < 0) newHealth = 0;
    player.setHealth(newHealth);
    
    FoodStatsMod.LOGGER.debug("应用永久加成: 血量比例={}, 新血量={}/{}", 
        healthRatio, newHealth, newMaxHealth);
    }

     @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        Block block = state.getBlock();

        // 检查配置中的方块食物
        List<String> blockFoodItems = (List<String>) Config.BLOCK_FOOD_ITEMS.get();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        if (blockId == null || !blockFoodItems.contains(blockId.toString())) return;

        // 特殊处理：蛋糕（多片食用）
        if (block instanceof CakeBlock) {
            handleCakeEat(player, event.getLevel(), pos, state);
        } else {
            // 一次性方块食物：直接记录（假定右键即食用）
            int nutrition = getNutritionForBlockFood(blockId);
            // 可选：进一步验证玩家饥饿值是否变化（此处简化）
            recordFoodEaten(player, blockId, nutrition);
        }
    }
private void handleCakeEat(Player player, Level level, BlockPos pos, BlockState state) {
        int oldBites = state.getValue(CakeBlock.BITES);
        if (oldBites >= 6) return; // 蛋糕已吃完

        level.getServer().execute(() -> {
            BlockState newState = level.getBlockState(pos);
            int newBites = newState.getValue(CakeBlock.BITES);
            if (newBites > oldBites) {
                onCakeEaten(player);
            }
        });
    }

 private void onCakeEaten(Player player) {
        ResourceLocation foodId = ResourceLocation.parse("minecraft:cake");
        IPlayerStats stats = FoodStatsMod.getPlayerStats(player);
        if (stats == null) return;

        // 获取蛋糕营养值（从配置读取）
        List<String> blockFoodItems = (List<String>) Config.BLOCK_FOOD_ITEMS.get();
        List<Integer> blockFoodNutrition = (List<Integer>) Config.BLOCK_FOOD_NUTRITION.get();
        int nutrition = 0;
        for (int i = 0; i < blockFoodItems.size(); i++) {
            if (blockFoodItems.get(i).equals(foodId.toString())) {
                nutrition = blockFoodNutrition.get(i);
                break;
            }
        }
        if (nutrition == 0) nutrition = 2; // 默认营养值

        // 记录食用
        if (!stats.getEatenFoods().contains(foodId)) {
            stats.getEatenFoods().add(foodId);
            player.displayClientMessage(Component.literal("§a新食物已记录"), true);
        }
        stats.addSaturation(nutrition);
        CHECK_THRESHOLD.accept(player, stats);
        FoodStatsMod.syncStats(player, stats);
    }
// 通用记录方法：增加饱食度、记录新食物、触发加成检查
    private void recordFoodEaten(Player player, ResourceLocation foodId, int nutrition) {
        List<String> blacklist = Config.FOOD_BLACKLIST.get();
        if (blacklist.contains(foodId.toString())) return;

        IPlayerStats stats = FoodStatsMod.getPlayerStats(player);
        if (stats == null) return;

        if (!stats.getEatenFoods().contains(foodId)) {
            stats.getEatenFoods().add(foodId);
            player.displayClientMessage(Component.literal("§a新食物已记录"), true);
        }
        stats.addSaturation(nutrition);
        CHECK_THRESHOLD.accept(player, stats);
        FoodStatsMod.syncStats(player, stats);
    }


     private int getNutritionForBlockFood(ResourceLocation blockId) {
        List<String> blockFoodItems = (List<String>) Config.BLOCK_FOOD_ITEMS.get();
        List<Integer> blockFoodNutrition = (List<Integer>) Config.BLOCK_FOOD_NUTRITION.get();
        for (int i = 0; i < blockFoodItems.size(); i++) {
            if (blockFoodItems.get(i).equals(blockId.toString())) {
                return blockFoodNutrition.get(i);
            }
        }
        return 2; // 默认营养值
    }
}