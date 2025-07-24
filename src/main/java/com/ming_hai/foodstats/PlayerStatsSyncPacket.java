package com.ming_hai.foodstats;

import com.ming_hai.foodstats.capability.IPlayerStats;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer; // 新增导入
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PlayerStatsSyncPacket {
    private final CompoundTag nbt;

    public PlayerStatsSyncPacket(IPlayerStats stats) {
        this.nbt = new CompoundTag();
        stats.saveNBT(this.nbt);
    }

    public PlayerStatsSyncPacket(FriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(nbt);
    }

    public static PlayerStatsSyncPacket decode(FriendlyByteBuf buf) {
        return new PlayerStatsSyncPacket(buf);
    }

   public void handle(Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && ClientPacketHandler.validateNBT(this.nbt)) {
                // 移除维度变化检测逻辑（移动到服务器端处理）
                player.getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
                    stats.loadNBT(this.nbt.copy());
                });
            }
        });
    });
    ctx.get().setPacketHandled(true);
}
    // 客户端专用处理类
    public static class ClientPacketHandler {
        public static void handlePacket(PlayerStatsSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) {
                    player.getCapability(FoodStatsMod.PLAYER_STATS).ifPresent(stats -> {
                        try {
                            if (validateNBT(packet.nbt)) {
                                stats.loadNBT(packet.nbt.copy());
                            }
                        } catch (Exception e) {
                            FoodStatsMod.LOGGER.error("Failed to load player stats", e);
                        }
                    });
                }
            });
            context.setPacketHandled(true);
        }

        // 修改为 public static
        public static boolean validateNBT(CompoundTag nbt) {
            return nbt.contains("TotalSaturation", Tag.TAG_INT) 
                && nbt.contains("BuffCount", Tag.TAG_INT)
                && nbt.contains("EatenFoods", Tag.TAG_LIST);
        }
    }
}
