package com.ming_hai.foodstats.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import com.ming_hai.foodstats.config.Config;
import java.util.List;


public class PlayerStats implements IPlayerStats {
    private int totalSaturation;
    private int buffCount; // 新增的 Buff 计数器
    private int healthBonus = 0; // 血量加成次数
    private int armorBonus = 0; // 护甲加成次数
    private int attackBonus = 0; // 攻击加成次数
    private final Set<ResourceLocation> eatenFoods = new HashSet<>();




@Override
    public int getCurrentSaturation() {
        int baseThreshold = Config.THRESHOLD.get();
        int thresholdIncrease = Config.THRESHOLD_INCREASE.get();
        
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

    // 新增的 Buff 计数方法
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

        eatenFoods.clear();
        List<? extends String> blacklist = Config.FOOD_BLACKLIST.get();
        ListTag foodsTag = tag.getList("EatenFoods", Tag.TAG_STRING);
        for (int i = 0; i < foodsTag.size(); i++) {
            String foodStr = foodsTag.getString(i);
            if (!blacklist.contains(foodStr)) {
                eatenFoods.add(new ResourceLocation(foodStr));
            }
        }
    }



    @Override
    public void reset() {
        totalSaturation = 0;
        buffCount = 0; // 重置计数器
        eatenFoods.clear();
    }

    @Override
    public void copyFrom(IPlayerStats other) {
        this.totalSaturation = other.getTotalSaturation();
        this.buffCount = other.getBuffCount(); // 复制计数器
       this.healthBonus = other.getHealthBonus();
    this.armorBonus = other.getArmorBonus();
    this.attackBonus = other.getAttackBonus();
    this.eatenFoods.clear();
    this.eatenFoods.addAll(other.getEatenFoods());
    }
    public int getUniqueFoodCount() {
        return eatenFoods.size();
    }
    
    
}