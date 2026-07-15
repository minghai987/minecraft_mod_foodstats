package com.ming_hai.foodstats.network;

import com.ming_hai.foodstats.FoodStatsMod;
import com.ming_hai.foodstats.config.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ConfigSyncPacket(CompoundTag data) implements CustomPacketPayload {
    public static final Type<ConfigSyncPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(FoodStatsMod.MODID, "config_sync")
    );

    public static final StreamCodec<FriendlyByteBuf, ConfigSyncPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, packet) -> buf.writeNbt(packet.data()),
            buf -> new ConfigSyncPacket(buf.readNbt())
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ConfigSyncPacket(Config.createSyncTag()));
    }

    public static void handle(final ConfigSyncPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> Config.applySyncTag(packet.data()));
    }
}
