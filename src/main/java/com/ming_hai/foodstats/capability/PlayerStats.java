package com.ming_hai.foodstats.capability;

import com.ming_hai.foodstats.FoodStatsMod;
import com.ming_hai.foodstats.config.Config;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.attachment.AttachmentType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class PlayerStats implements IPlayerStats {
    public static final Codec<PlayerStats> CODEC = RecordCodecBuilder.create(instance -> 
        instance.group(
            Codec.INT.fieldOf("totalSaturation").forGetter(PlayerStats::getTotalSaturation),
            Codec.INT.fieldOf("buffCount").forGetter(PlayerStats::getBuffCount),
            Codec.INT.fieldOf("healthBonus").forGetter(PlayerStats::getHealthBonus),
            Codec.INT.fieldOf("armorBonus").forGetter(PlayerStats::getArmorBonus),
            Codec.INT.fieldOf("attackBonus").forGetter(PlayerStats::getAttackBonus),
            Codec.INT.optionalFieldOf("uniqueFoodProgress", 0).forGetter(PlayerStats::getUniqueFoodProgress),
            Codec.list(Codec.STRING).fieldOf("eatenFoods").forGetter(stats -> 
                stats.getEatenFoods().stream().map(ResourceLocation::toString).toList())
        ).apply(instance, PlayerStats::new)
    );
    
    // 定义附件类型 - 使用正确的构建器
    public static final Supplier<AttachmentType<PlayerStats>> ATTACHMENT_TYPE = 
        () -> AttachmentType.<PlayerStats>builder(() -> new PlayerStats())
            .serialize(CODEC)
            .copyOnDeath()
            .build();

    private int totalSaturation;
    private int buffCount;
    private int healthBonus;
    private int armorBonus;
    private int attackBonus;
    private int uniqueFoodProgress;
    private final Set<ResourceLocation> eatenFoods;

    // 用于 Codec 的构造函数
    public PlayerStats(int totalSaturation, int buffCount, int healthBonus, int armorBonus, int attackBonus, int uniqueFoodProgress, List<String> eatenFoodsStr) {
        this.totalSaturation = totalSaturation;
        this.buffCount = buffCount;
        this.healthBonus = healthBonus;
        this.armorBonus = armorBonus;
        this.attackBonus = attackBonus;
        this.uniqueFoodProgress = uniqueFoodProgress;
        this.eatenFoods = new HashSet<>();
        
        for (String foodStr : eatenFoodsStr) {
            this.eatenFoods.add(ResourceLocation.parse(foodStr));
        }
    }

    public PlayerStats() {
        this(0, 0, 0, 0, 0, 0, List.of());
    }

    @Override
    public int getCurrentSaturation() {
        int baseThreshold = Config.getThreshold();
        int thresholdIncrease = Config.getThresholdIncrease();
        
        int consumedSaturation = 0;
        for (int i = 0; i < buffCount; i++) {
            consumedSaturation += baseThreshold + i * thresholdIncrease;
        }
        
        return totalSaturation - consumedSaturation;
    }

    @Override
    public int getTotalSaturation() {
        return totalSaturation;
    }

    @Override
    public void addSaturation(int value) {
        totalSaturation = Math.max(0, totalSaturation + value);
    }

    @Override
    public Set<ResourceLocation> getEatenFoods() {
        return eatenFoods;
    }

    @Override
    public int getBuffCount() {
        return buffCount;
    }

    @Override
    public int getHealthBonus() {
        return healthBonus;
    }

    @Override
    public void setHealthBonus(int count) {
        healthBonus = count;
    }

    @Override
    public int getArmorBonus() {
        return armorBonus;
    }

    @Override
    public void setArmorBonus(int count) {
        armorBonus = count;
    }

    @Override
    public int getAttackBonus() {
        return attackBonus;
    }

    @Override
    public void setAttackBonus(int count) {
        attackBonus = count;
    }

    @Override
    public int getUniqueFoodProgress() {
        return uniqueFoodProgress;
    }

    @Override
    public void setUniqueFoodProgress(int count) {
        uniqueFoodProgress = Math.max(0, count);
    }

    @Override
    public void incrementUniqueFoodProgress() {
        uniqueFoodProgress++;
    }

    @Override
    public void incrementBuffCount() {
        buffCount++;
    }

    @Override
    public void setBuffCount(int count) {
        buffCount = count;
    }

    @Override
    public void saveNBT(CompoundTag tag) {
        tag.putInt("TotalSaturation", totalSaturation);
        tag.putInt("BuffCount", buffCount);
        tag.putInt("HealthBonus", healthBonus);
        tag.putInt("ArmorBonus", armorBonus);
        tag.putInt("AttackBonus", attackBonus);
        tag.putInt("UniqueFoodProgress", uniqueFoodProgress);
        
        ListTag foodsTag = new ListTag();
        for (ResourceLocation food : eatenFoods) {
            foodsTag.add(StringTag.valueOf(food.toString()));
        }
        tag.put("EatenFoods", foodsTag);
    }

    @Override
    public void loadNBT(CompoundTag tag) {
        totalSaturation = tag.getInt("TotalSaturation");
        buffCount = tag.getInt("BuffCount");
        healthBonus = tag.getInt("HealthBonus");
        armorBonus = tag.getInt("ArmorBonus");
        attackBonus = tag.getInt("AttackBonus");
        uniqueFoodProgress = tag.getInt("UniqueFoodProgress");

        eatenFoods.clear();
        ListTag foodsTag = tag.getList("EatenFoods", Tag.TAG_STRING);
        for (int i = 0; i < foodsTag.size(); i++) {
            String foodStr = foodsTag.getString(i);
            eatenFoods.add(ResourceLocation.parse(foodStr));
        }
    }

    @Override
    public void reset() {
        totalSaturation = 0;
        buffCount = 0;
        healthBonus = 0;
        armorBonus = 0;
        attackBonus = 0;
        uniqueFoodProgress = 0;
        eatenFoods.clear();
    }

@Override
public void copyFrom(IPlayerStats other) {
    this.totalSaturation = other.getTotalSaturation();
    this.buffCount = other.getBuffCount();
    this.healthBonus = other.getHealthBonus();
    this.armorBonus = other.getArmorBonus();
    this.attackBonus = other.getAttackBonus();
    this.uniqueFoodProgress = other.getUniqueFoodProgress();
    
    // 清空并重新添加所有食物
    this.eatenFoods.clear();
    this.eatenFoods.addAll(other.getEatenFoods());
    
    FoodStatsMod.LOGGER.debug("PlayerStats 数据复制完成: 饱食度={}, 加成次数={}, 食物数量={}", 
        totalSaturation, buffCount, eatenFoods.size());
}

    public int getUniqueFoodCount() {
        return eatenFoods.size();
    }
}