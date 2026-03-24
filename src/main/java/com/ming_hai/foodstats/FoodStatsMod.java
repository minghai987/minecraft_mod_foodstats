// FoodStatsMod.java
package com.ming_hai.foodstats;

import com.ming_hai.foodstats.capability.IPlayerStats;
import com.ming_hai.foodstats.capability.PlayerStats;
import com.ming_hai.foodstats.config.Config;
import com.ming_hai.foodstats.items.FoodGuideItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import com.ming_hai.foodstats.network.PlayerStatsSyncPacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.Supplier;

@Mod("foodstats")
public class FoodStatsMod {
    public static final String MODID = "foodstats";
    public static final Logger LOGGER = LogManager.getLogger(MODID);


    
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MODID);
    public static final Supplier<Item> FOOD_GUIDE = ITEMS.register("food_guide", FoodGuideItem::new);
    
    // 注册附件类型
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = 
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);
    
    public static final Supplier<AttachmentType<PlayerStats>> PLAYER_STATS = 
        ATTACHMENT_TYPES.register("player_stats", PlayerStats.ATTACHMENT_TYPE);

    public FoodStatsMod(IEventBus modEventBus, ModContainer modContainer) {
        // 注册配置
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        
        // 注册物品和附件类型
        ITEMS.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);
        
        // 注册事件监听器
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addToCreativeTab);
        modEventBus.addListener(this::registerPayloadHandlers);
        
        // 注册事件处理器
        NeoForge.EVENT_BUS.register(new com.ming_hai.foodstats.events.PlayerEvents());
        NeoForge.EVENT_BUS.register(this);
      
    }
    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(FoodStatsMod.MODID);
        
        // 注册 PlayerStatsSyncPacket
        registrar.playToClient(
            PlayerStatsSyncPacket.TYPE,
            PlayerStatsSyncPacket.STREAM_CODEC,
            PlayerStatsSyncPacket::handle
        );
    }


   
   // FoodStatsMod.java - 修复版本
private void commonSetup(final FMLCommonSetupEvent event) {
    event.enqueueWork(() -> {
        // 修复：使用正确的类名
        if (com.ming_hai.foodstats.compat.JEIIntegration.isJeiLoaded()) {
            com.ming_hai.foodstats.compat.JEIIntegration.logJeiInfo();
            LOGGER.info("JEI 已加载，配方查看功能可用");
        } else {
            LOGGER.info("JEI 未加载，配方查看功能不可用");
        }

        LOGGER.info("食物手册配方应该位于: data/foodstats/recipes/food_guide.json");
        LOGGER.info("物品注册ID: foodstats:food_guide");
    });
}

    private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(FOOD_GUIDE.get());
        }
    }

    

    // 简化版：暂时不进行网络同步
    public static void syncStats(Player player, IPlayerStats stats) {
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PlayerStatsSyncPacket.sendToPlayer(serverPlayer, stats);
            LOGGER.debug("已同步玩家数据到客户端");
        }
    }

    public static IPlayerStats getPlayerStats(Player player) {
        return player.getData(PLAYER_STATS);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) {
            IPlayerStats stats = getPlayerStats(player);
            if (stats != null) {
                syncStats(player, stats);
                LOGGER.debug("维度改变，已同步玩家数据");
            }
        }
    }
}