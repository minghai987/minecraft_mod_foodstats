package com.ming_hai.foodstats.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import java.util.HashSet;
import java.util.Set;

public class PlayerStats implements IPlayerStats {
    private int totalSaturation;
    private int buffCount; // 新增的 Buff 计数器
    private final Set<ResourceLocation> eatenFoods = new HashSet<>();

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
        tag.putInt("BuffCount", buffCount); // 保存计数器
        
        CompoundTag foodsTag = new CompoundTag();
        int i = 0;
        for (ResourceLocation food : eatenFoods) {
            foodsTag.putString("Food_" + i++, food.toString());
        }
        tag.put("EatenFoods", foodsTag);
    }

    @Override
    public void loadNBT(CompoundTag tag) {
        totalSaturation = tag.getInt("TotalSaturation");
        buffCount = tag.getInt("BuffCount"); // 加载计数器
        
        CompoundTag foodsTag = tag.getCompound("EatenFoods");
        eatenFoods.clear();
        for (String key : foodsTag.getAllKeys()) {
            if (key.startsWith("Food_")) {
                eatenFoods.add(new ResourceLocation(foodsTag.getString(key)));
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
        this.eatenFoods.clear();
        this.eatenFoods.addAll(other.getEatenFoods());
    }
}