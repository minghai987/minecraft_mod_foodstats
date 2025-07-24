package com.ming_hai.foodstats;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class RequestResyncPacket {
    public RequestResyncPacket() {} // 无参构造函数

    public RequestResyncPacket(FriendlyByteBuf buf) {} // 添加带参数的构造函数
    
    public void encode(FriendlyByteBuf buf) {}
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                player.getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
                    FoodStatsMod.syncStats(player, stats);
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}