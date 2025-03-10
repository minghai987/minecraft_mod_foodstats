package com.ming_hai.foodstats;

import com.ming_hai.foodstats.capability.IPlayerStats;
import com.ming_hai.foodstats.capability.PlayerStats;
import com.ming_hai.foodstats.config.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

@Mod("foodstats")
public class FoodStatsMod {
    public static final String MODID = "foodstats";
    public static final Capability<IPlayerStats> PLAYER_STATS = CapabilityManager.get(new CapabilityToken<>() {});

    public FoodStatsMod() {
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, Config.SPEC);

        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(this::registerCapabilities); // 注册能力事件监听

        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, this::attachCapabilities);
        MinecraftForge.EVENT_BUS.register(new com.ming_hai.foodstats.events.PlayerEvents());
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IPlayerStats.class); // 第38行：新的事件驱动注册
    }

    private void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(new ResourceLocation(MODID, "player_stats"), new ICapabilityProvider() {
                final LazyOptional<IPlayerStats> instance = LazyOptional.of(PlayerStats::new);

                @Nonnull
                @Override
                public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
                    return PLAYER_STATS.orEmpty(cap, instance);
                }
            });
        }
    }
}