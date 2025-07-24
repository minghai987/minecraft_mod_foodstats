package com.ming_hai.foodstats;

import com.ming_hai.foodstats.FoodStatsMod.ClientProxy;
import com.ming_hai.foodstats.FoodStatsMod.CommonProxy;
// 移除JEI的直接导入
import com.ming_hai.foodstats.capability.IPlayerStats;
import com.ming_hai.foodstats.capability.PlayerStats;
import com.ming_hai.foodstats.config.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import com.ming_hai.foodstats.items.FoodGuideItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Rarity;
import java.util.Comparator;
import java.util.stream.Collectors;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.ModList;
import com.ming_hai.foodstats.capability.PlayerStats;
import net.minecraft.client.Minecraft;

@Mod("foodstats")
public class FoodStatsMod {
    public static final String MODID = "foodstats";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static boolean JEI_LOADED = false;
    public static Object jeiRuntime;
    public static final Capability<IPlayerStats> PLAYER_STATS = CapabilityManager.get(new CapabilityToken<>() {});
    private static final String PROTOCOL_VERSION = "1.0";
    
    public static boolean isJeiLoaded() {
        return ModList.get().isLoaded("jei");
    }
    
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    private static final DeferredRegister<Item> ITEMS = 
    DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public void setupProxy(FMLCommonSetupEvent event) {
        PROXY = DistExecutor.safeRunForDist(
            () -> ClientProxy::new,
            () -> CommonProxy::new
        );
        
        JEI_LOADED = isJeiLoaded();
        if (JEI_LOADED) {
            LOGGER.info("JEI integration enabled");
        }
    }

    public FoodStatsMod() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(this::registerCapabilities);
        eventBus.addListener(this::setupProxy);
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, this::attachCapabilities);
        MinecraftForge.EVENT_BUS.register(new com.ming_hai.foodstats.events.PlayerEvents());
        MinecraftForge.EVENT_BUS.register(this); 
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::addToCreativeTab);
        
        CHANNEL.registerMessage(0, PlayerStatsSyncPacket.class,
                PlayerStatsSyncPacket::encode,
                PlayerStatsSyncPacket::decode,
                PlayerStatsSyncPacket::handle);
        
        CHANNEL.registerMessage(1, RequestResyncPacket.class,
                (msg, buf) -> msg.encode(buf),
                RequestResyncPacket::new,
                RequestResyncPacket::handle);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IPlayerStats.class);
    }
    public static void setJeiRuntime(Object runtime) {
    if (runtime == null) {
        JEI_LOADED = false;
        return;
    }
    try {
        if (Class.forName("mezz.jei.api.runtime.IJeiRuntime").isInstance(runtime)) {
            jeiRuntime = runtime;
        } else {
            LOGGER.warn("Received invalid JEI runtime object");
        }
    } catch (ClassNotFoundException e) {
        LOGGER.error("JEI classes not available");
        JEI_LOADED = false;
    }
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

   // 在 FoodStatsMod.java 中修改
public static void syncStats(Player player, IPlayerStats stats) {
    if (player instanceof ServerPlayer serverPlayer) {
        serverPlayer.server.execute(() -> {
            // 直接保存到玩家实体
            CompoundTag persistentData = player.getPersistentData();
            CompoundTag playerData = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
            
            CompoundTag statsTag = new CompoundTag();
            stats.saveNBT(statsTag);
            playerData.put("FoodStatsData", statsTag);
            persistentData.put(Player.PERSISTED_NBT_TAG, playerData);
            
            // 发送同步包
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                new PlayerStatsSyncPacket(stats));
        });
    }
}

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class CommonProxy {
        public void openFoodGuideScreen(Player player) {}
    }

    public static class ClientProxy extends CommonProxy {
    @Override
    public void openFoodGuideScreen(Player player) {
        // 添加能力存在性检查
        LazyOptional<IPlayerStats> statsCap = player.getCapability(PLAYER_STATS);
        if (statsCap.isPresent()) {
            statsCap.ifPresent(stats -> {
                // 确保在主线程执行
                Minecraft.getInstance().tell(() -> {
                    Minecraft.getInstance().setScreen(new com.ming_hai.foodstats.client.screens.FoodGuideScreen(stats));
                });
            });
        } else {
            LOGGER.error("Player capability not found for: " + player.getName().getString());
        }
    }
}


    public static CommonProxy PROXY = DistExecutor.safeRunForDist(
            () -> ClientProxy::new, 
            () -> CommonProxy::new
        );

    public static final RegistryObject<Item> FOOD_GUIDE = ITEMS.register("food_guide",
            () -> new FoodGuideItem());



    
   private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
    if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
        event.accept(FOOD_GUIDE.get());
    }
}
    @SubscribeEvent
public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
    Player player = event.getEntity();
    if (!player.level().isClientSide) {
        player.getCapability(PLAYER_STATS).ifPresent(stats -> {
            syncStats(player, stats);
        });
    }
}
}