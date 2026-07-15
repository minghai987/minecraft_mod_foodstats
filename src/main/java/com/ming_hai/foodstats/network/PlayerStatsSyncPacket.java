// PlayerStatsSyncPacket.java - 修复版本
package com.ming_hai.foodstats.network;

import com.ming_hai.foodstats.FoodStatsMod;
import com.ming_hai.foodstats.capability.IPlayerStats;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PlayerStatsSyncPacket(CompoundTag data) implements CustomPacketPayload {
    public static final Type<PlayerStatsSyncPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(FoodStatsMod.MODID, "player_stats_sync")
    );

    // 使用正确的 StreamCodec 创建方式
    public static final StreamCodec<FriendlyByteBuf, PlayerStatsSyncPacket> STREAM_CODEC = 
        StreamCodec.of(
            (buf, packet) -> buf.writeNbt(packet.data()), // 编码器
            buf -> new PlayerStatsSyncPacket(buf.readNbt())  // 解码器
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // 发送给玩家
    public static void sendToPlayer(ServerPlayer player, IPlayerStats stats) {
        CompoundTag tag = new CompoundTag();
        stats.saveNBT(tag);
        PacketDistributor.sendToPlayer(player, new PlayerStatsSyncPacket(tag));
    }

    // 处理包的方法
    public static void handle(final PlayerStatsSyncPacket packet, final IPayloadContext context) {
        // 确保在客户端线程上执行
        context.enqueueWork(() -> {
            // 客户端处理数据
            com.ming_hai.foodstats.client.ClientDataHandler.handleStatsSync(packet.data);
        });
    }
}