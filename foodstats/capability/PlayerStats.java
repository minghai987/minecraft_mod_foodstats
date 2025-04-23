package com.ming_hai.foodstats.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import com.ming_hai.foodstats.config.Config;



public class PlayerStats implements IPlayerStats {
    private int totalSaturation;
    private int buffCount; // 新增的 Buff 计数器
    private final Set<ResourceLocation> eatenFoods = new HashSet<>();
    public int getCurrentSaturation() {
        return totalSaturation % Config.THRESHOLD.get();
    }
    
    
    @Override
    public int getTotalSaturation() {
        return totalSaturation;
    }

    @Override
    public void addSaturation(int value) {
        totalSaturation += value;
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
    
    eatenFoods.clear();
    ListTag foodsTag = tag.getList("EatenFoods", Tag.TAG_STRING);
    for (int i = 0; i < foodsTag.size(); i++) {
        eatenFoods.add(new ResourceLocation(foodsTag.getString(i)));
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
        this.eatenFoods.clear();
        this.eatenFoods.addAll(other.getEatenFoods());
    }
    public int getUniqueFoodCount() {
        return eatenFoods.size();
    }
    
    
}