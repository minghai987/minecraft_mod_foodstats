package com.ming_hai.foodstats.events;

import com.ming_hai.foodstats.FoodStatsMod;
import com.ming_hai.foodstats.capability.IPlayerStats;
import com.ming_hai.foodstats.compat.TavernDrinkCompat;
import com.ming_hai.foodstats.config.Config;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = FoodStatsMod.MODID)
public class CommandEvents {
    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(
            Commands.literal("foodstats")
                .then(Commands.literal("info")
                    .executes(ctx -> showPlayerInfo(ctx.getSource())))
                .then(Commands.literal("reset")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> resetPlayerStats(ctx.getSource())))
                .then(Commands.literal("mode")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> showBonusMode(ctx.getSource()))
                    .then(Commands.literal("saturation")
                        .executes(ctx -> setBonusMode(ctx.getSource(), Config.BonusMode.SATURATION_THRESHOLD)))
                    .then(Commands.literal("unique")
                        .executes(ctx -> setBonusMode(ctx.getSource(), Config.BonusMode.EACH_UNIQUE_FOOD)))
                    .then(Commands.literal("count")
                        .executes(ctx -> setBonusMode(ctx.getSource(), Config.BonusMode.UNIQUE_FOOD_COUNT_THRESHOLD)))
                    .then(Commands.literal("饱食度")
                        .executes(ctx -> setBonusMode(ctx.getSource(), Config.BonusMode.SATURATION_THRESHOLD)))
                    .then(Commands.literal("每种")
                        .executes(ctx -> setBonusMode(ctx.getSource(), Config.BonusMode.EACH_UNIQUE_FOOD)))
                    .then(Commands.literal("累计")
                        .executes(ctx -> setBonusMode(ctx.getSource(), Config.BonusMode.UNIQUE_FOOD_COUNT_THRESHOLD))))
                .then(Commands.literal("whitelist")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("addheld")
                        .executes(ctx -> addHeldFood(ctx.getSource(), true)))
                    .then(Commands.literal("removeheld")
                        .executes(ctx -> removeHeldFood(ctx.getSource(), true)))
                    .then(Commands.literal("clear")
                        .executes(ctx -> clearList(ctx.getSource(), true)))
                    .then(Commands.literal("list")
                        .executes(ctx -> listFoods(ctx.getSource(), true))))
                .then(Commands.literal("blacklist")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("addheld")
                        .executes(ctx -> addHeldFood(ctx.getSource(), false)))
                    .then(Commands.literal("removeheld")
                        .executes(ctx -> removeHeldFood(ctx.getSource(), false)))
                    .then(Commands.literal("clear")
                        .executes(ctx -> clearList(ctx.getSource(), false)))
                    .then(Commands.literal("list")
                        .executes(ctx -> listFoods(ctx.getSource(), false))))
                .then(Commands.literal("reload")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        Config.refreshCaches();
                        FoodStatsMod.syncConfigToAll(ctx.getSource().getServer());
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "§a食物增强配置已刷新。白名单 " + Config.FOOD_WHITELIST.get().size() +
                                " 项，黑名单 " + Config.FOOD_BLACKLIST.get().size() + " 项。"), false);
                        return 1;
                    }))
        );
    }

    private static int showPlayerInfo(CommandSourceStack source) throws CommandSyntaxException {
        Player player = source.getPlayerOrException();
        IPlayerStats stats = FoodStatsMod.getPlayerStats(player);
        if (stats == null) {
            source.sendFailure(Component.literal("无法读取玩家食物统计数据。"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(buildPlayerInfo(stats)), false);
        return 1;
    }

    private static int resetPlayerStats(CommandSourceStack source) throws CommandSyntaxException {
        Player player = source.getPlayerOrException();
        IPlayerStats stats = FoodStatsMod.getPlayerStats(player);
        if (stats == null) {
            source.sendFailure(Component.literal("无法读取玩家食物统计数据。"));
            return 0;
        }

        stats.reset();
        PlayerEvents.applyPermanentBonus(player, 0, 0, 0);
        FoodStatsMod.syncStats(player, stats);
        source.sendSuccess(() -> Component.literal("§a已重置食物统计数据，并移除当前属性加成。"), false);
        return 1;
    }

    private static String buildPlayerInfo(IPlayerStats stats) {
        return "§a[食物统计]" +
            "§r\n增益方式: §e" + Config.getBonusMode().getDisplayName() +
            "§r\n已记录食物数量: §e" + stats.getEatenFoods().size() +
            "§r\n触发加成次数: §e" + stats.getBuffCount() +
            "§r\n当前属性加成:" +
            formatBonusLine("血量", Config.getHealthBonusValue(), stats.getHealthBonus(), Config.getHealthMax()) +
            formatBonusLine("护甲", Config.getArmorBonusValue(), stats.getArmorBonus(), Config.getArmorMax()) +
            formatBonusLine("攻击", Config.getAttackBonusValue(), stats.getAttackBonus(), Config.getAttackMax()) +
            getModeProgressLine(stats);
    }

    private static String formatBonusLine(String name, double valuePerBonus, int bonusCount, int maxCount) {
        int effectiveCount = maxCount > 0 ? Math.min(bonusCount, maxCount) : bonusCount;
        String limitText = maxCount > 0 ? "/" + maxCount : "";
        return "§r\n" + name + ": §e+" + String.format("%.1f", valuePerBonus * effectiveCount) +
            " §7(" + bonusCount + limitText + "次)";
    }

    private static String getModeProgressLine(IPlayerStats stats) {
        if (Config.getBonusMode() == Config.BonusMode.SATURATION_THRESHOLD) {
            int nextThreshold = Config.getThreshold() + stats.getBuffCount() * Config.getThresholdIncrease();
            return "§r\n当前累计饱食度: §e" + stats.getCurrentSaturation() + "/" + nextThreshold;
        }
        if (Config.getBonusMode() == Config.BonusMode.UNIQUE_FOOD_COUNT_THRESHOLD) {
            return "§r\n累计新食物进度: §e" + stats.getUniqueFoodProgress() + "/" + Config.getUniqueFoodsPerBonus();
        }
        return "§r\n新食物增益: §e每种新的允许食物触发一次";
    }

    private static int showBonusMode(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(
            "§a当前食用模式: §e" + Config.getBonusMode().getDisplayName() +
                "§r\n§7可用指令: /foodstats mode saturation | unique | count" +
                "§r\n§7中文别名: /foodstats mode 饱食度 | 每种 | 累计"), false);
        return 1;
    }

    private static int setBonusMode(CommandSourceStack source, Config.BonusMode mode) {
        Config.setBonusMode(mode);
        FoodStatsMod.syncConfigToAll(source.getServer());
        source.sendSuccess(() -> Component.literal("§a已切换食用模式为: §e" + mode.getDisplayName()), true);
        return 1;
    }

    private static int addHeldFood(CommandSourceStack source, boolean whitelist) throws CommandSyntaxException {
        String foodId = getHeldFoodId(source.getPlayerOrException(), source);
        if (foodId == null) {
            return 0;
        }
        if (whitelist) {
            Config.addFoodToWhitelist(foodId);
        } else {
            Config.addFoodToBlacklist(foodId);
        }
        FoodStatsMod.syncConfigToAll(source.getServer());
        source.sendSuccess(() -> Component.literal("§a已将 " + getFoodDisplayName(foodId) + " 加入" + getListName(whitelist)), false);
        return 1;
    }

    private static int removeHeldFood(CommandSourceStack source, boolean whitelist) throws CommandSyntaxException {
        String foodId = getHeldFoodId(source.getPlayerOrException(), source);
        if (foodId == null) {
            return 0;
        }
        if (whitelist) {
            Config.removeFoodFromWhitelist(foodId);
        } else {
            Config.removeFoodFromBlacklist(foodId);
        }
        FoodStatsMod.syncConfigToAll(source.getServer());
        source.sendSuccess(() -> Component.literal("§a已将 " + getFoodDisplayName(foodId) + " 移出" + getListName(whitelist)), false);
        return 1;
    }

    private static int clearList(CommandSourceStack source, boolean whitelist) {
        if (whitelist) {
            Config.clearWhitelist();
        } else {
            Config.clearBlacklist();
        }
        FoodStatsMod.syncConfigToAll(source.getServer());
        source.sendSuccess(() -> Component.literal("§a已清空" + getListName(whitelist)), false);
        return 1;
    }

    private static int listFoods(CommandSourceStack source, boolean whitelist) {
        List<? extends String> foods = whitelist ? Config.FOOD_WHITELIST.get() : Config.FOOD_BLACKLIST.get();
        if (foods.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e" + getListName(whitelist) + "为空。"), false);
            return 1;
        }
        String names = foods.stream()
            .map(CommandEvents::getFoodDisplayName)
            .collect(Collectors.joining("、"));
        source.sendSuccess(() -> Component.literal("§a" + getListName(whitelist) + ": §r" + names), false);
        return 1;
    }

    private static String getHeldFoodId(Player player, CommandSourceStack source) {
        ItemStack stack = player.getMainHandItem();
        // 允许主手普通食物或可计数酒馆酒加入名单
        boolean recordable = !stack.isEmpty() && (
            stack.getFoodProperties(player) != null || TavernDrinkCompat.isCountableDrink(stack.getItem())
        );
        if (!recordable) {
            source.sendFailure(Component.literal("请先在主手拿着一个食物或可记录的酒馆酒。"));
            return null;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            source.sendFailure(Component.literal("无法识别主手物品。"));
            return null;
        }
        return id.toString();
    }

    private static String getFoodDisplayName(String foodId) {
        ResourceLocation id = ResourceLocation.tryParse(foodId);
        if (id == null) {
            return foodId;
        }
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
        return item.map(value -> new ItemStack(value).getHoverName().getString()).orElse(foodId);
    }

    private static String getListName(boolean whitelist) {
        return whitelist ? "食物白名单" : "食物黑名单";
    }
}