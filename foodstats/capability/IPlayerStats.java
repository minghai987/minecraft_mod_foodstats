package com.ming_hai.foodstats.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import java.util.Set;

public interface IPlayerStats {
    int getTotalSaturation();
    void addSaturation(int value);
    Set<ResourceLocation> getEatenFoods();
    
    // 新增 Buff 计数方法
    int getBuffCount();
    void incrementBuffCount();
    void setBuffCount(int count);

    void saveNBT(CompoundTag tag);
    void loadNBT(CompoundTag tag);
    void reset();
    void copyFrom(IPlayerStats other);
}