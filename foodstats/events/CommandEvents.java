package com.ming_hai.foodstats.events;

import com.mojang.brigadier.CommandDispatcher;
import com.ming_hai.foodstats.FoodStatsMod;
import com.ming_hai.foodstats.capability.IPlayerStats;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FoodStatsMod.MODID)
public class CommandEvents {
    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(
            Commands.literal("foodstats")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("info")
                    .executes(ctx -> {
                        Player player = ctx.getSource().getPlayerOrException();
                        player.getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
                            ctx.getSource().sendSuccess(
                                () -> Component.literal("§a[食物统计]§r\n已累积饱食度: §e" + stats.getTotalSaturation() + "§r\n独特食物数量: §e" + stats.getEatenFoods().size()),
                                false
                            );
                        });
                        return 1;
                    }))
                .then(Commands.literal("reset")
                    .executes(ctx -> {
                        Player player = ctx.getSource().getPlayerOrException();
                        player.getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
                            stats.reset(); // 直接调用reset方法
                            ctx.getSource().sendSuccess(
                                () -> Component.literal("§a已重置食物统计数据"),
                                false
                            );
                        });
                        return 1;
                    }))
        );
    }
}