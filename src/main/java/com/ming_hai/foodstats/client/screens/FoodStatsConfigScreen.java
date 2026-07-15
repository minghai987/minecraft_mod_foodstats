package com.ming_hai.foodstats.client.screens;

import com.ming_hai.foodstats.config.Config;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class FoodStatsConfigScreen {
    private static final String NO_SELECTION = "请选择食物";
    private final Map<String, String> displayToId = new HashMap<>();
    private final Map<String, String> idToDisplay = new HashMap<>();
    private Screen parent;
    private List<String> pendingWhitelist;
    private List<String> pendingBlacklist;
    private List<String> originalWhitelist;
    private List<String> originalBlacklist;

    public static Screen create(Screen parent) {
        return new FoodStatsConfigScreen().build(parent);
    }

    private Screen build(Screen parent) {
        this.parent = parent;
        initPendingLists();
        buildFoodNameMaps();

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.literal("食物增强配置"))
            .setSavingRunnable(this::savePendingLists)
            .setAfterInitConsumer(FoodStatsConfigScreen::hideTopSearchBox);
        ConfigEntryBuilder entries = builder.entryBuilder();

        addBasicEntries(builder.getOrCreateCategory(Component.literal("基础设置")), entries);
        addListEntries(builder.getOrCreateCategory(Component.literal("食物名单")), entries);
        addBonusEntries(builder.getOrCreateCategory(Component.literal("属性加成")), entries);
        addMessageEntries(builder.getOrCreateCategory(Component.literal("消息设置")), entries);

        return builder.build();
    }

    private void addBasicEntries(ConfigCategory category, ConfigEntryBuilder entries) {
        category.addEntry(entries.startBooleanToggle(Component.literal("首次加入给予食物手册"), Config.START_WITH_GUIDE.get())
            .setDefaultValue(true)
            .setSaveConsumer(Config.START_WITH_GUIDE::set)
            .build());
        category.addEntry(entries.startEnumSelector(Component.literal("增益方式"), Config.BonusMode.class, Config.getBonusMode())
            .setDefaultValue(Config.BonusMode.SATURATION_THRESHOLD)
            .setTooltip(Component.literal("选择食物增强触发增益的计算方式。"))
            .setSaveConsumer(Config.BONUS_MODE::set)
            .build());
        category.addEntry(entries.startIntField(Component.literal("触发所需饱食度"), Config.THRESHOLD.get())
            .setDefaultValue(20)
            .setMin(1)
            .setMax(1000)
            .setTooltip(Component.literal("仅在“按饱食度累计”模式生效。"))
            .setSaveConsumer(Config.THRESHOLD::set)
            .build());
        category.addEntry(entries.startIntField(Component.literal("每次触发后阈值增加"), Config.THRESHOLD_INCREASE.get())
            .setDefaultValue(0)
            .setMin(0)
            .setMax(1000)
            .setTooltip(Component.literal("仅在“按饱食度累计”模式生效，0 表示不增加。"))
            .setSaveConsumer(Config.THRESHOLD_INCREASE::set)
            .build());
        category.addEntry(entries.startIntField(Component.literal("累计多少种新食物触发一次"), Config.UNIQUE_FOODS_PER_BONUS.get())
            .setDefaultValue(10)
            .setMin(1)
            .setMax(1000)
            .setTooltip(Component.literal("仅在“每若干种新食物一次”模式生效，重复食用同一种不计数。"))
            .setSaveConsumer(Config.UNIQUE_FOODS_PER_BONUS::set)
            .build());
    }

    private void addListEntries(ConfigCategory category, ConfigEntryBuilder entries) {
        category.addEntry(entries.startTextDescription(Component.literal("白名单为空时不限制食物；白名单非空时，只有白名单食物会触发增益。黑名单优先级高于白名单。")).build());

        category.addEntry(createFoodDropdown("添加到食物白名单", allFoodDisplays(), selected -> addDisplayToList(selected, true)));
        category.addEntry(createPendingFoodList("当前食物白名单", true));

        category.addEntry(createFoodDropdown("添加到食物黑名单", allFoodDisplays(), selected -> addDisplayToList(selected, false)));
        category.addEntry(createPendingFoodList("当前食物黑名单", false));
    }

    private void addBonusEntries(ConfigCategory category, ConfigEntryBuilder entries) {
        category.addEntry(entries.startDoubleField(Component.literal("血量加成"), Config.HEALTH_BONUS.get())
            .setDefaultValue(1.0)
            .setMin(0.0)
            .setMax(100.0)
            .setSaveConsumer(Config.HEALTH_BONUS::set)
            .build());
        category.addEntry(entries.startDoubleField(Component.literal("护甲加成"), Config.ARMOR_BONUS.get())
            .setDefaultValue(1.0)
            .setMin(0.0)
            .setMax(100.0)
            .setSaveConsumer(Config.ARMOR_BONUS::set)
            .build());
        category.addEntry(entries.startDoubleField(Component.literal("攻击加成"), Config.ATTACK_BONUS.get())
            .setDefaultValue(1.0)
            .setMin(0.0)
            .setMax(100.0)
            .setSaveConsumer(Config.ATTACK_BONUS::set)
            .build());
        category.addEntry(entries.startIntField(Component.literal("血量加成上限次数"), Config.HEALTH_MAX.get())
            .setDefaultValue(0)
            .setMin(0)
            .setMax(1000)
            .setTooltip(Component.literal("0 表示无上限。"))
            .setSaveConsumer(Config.HEALTH_MAX::set)
            .build());
        category.addEntry(entries.startIntField(Component.literal("护甲加成上限次数"), Config.ARMOR_MAX.get())
            .setDefaultValue(0)
            .setMin(0)
            .setMax(1000)
            .setTooltip(Component.literal("0 表示无上限。"))
            .setSaveConsumer(Config.ARMOR_MAX::set)
            .build());
        category.addEntry(entries.startIntField(Component.literal("攻击加成上限次数"), Config.ATTACK_MAX.get())
            .setDefaultValue(0)
            .setMin(0)
            .setMax(1000)
            .setTooltip(Component.literal("0 表示无上限。"))
            .setSaveConsumer(Config.ATTACK_MAX::set)
            .build());
        category.addEntry(entries.startIntField(Component.literal("总触发次数上限"), Config.MAX_BUFF_COUNT.get())
            .setDefaultValue(0)
            .setMin(0)
            .setMax(1000)
            .setTooltip(Component.literal("0 表示无上限。"))
            .setSaveConsumer(Config.MAX_BUFF_COUNT::set)
            .build());
        category.addEntry(entries.startDoubleField(Component.literal("初始血量"), Config.BASE_HEALTH.get())
            .setDefaultValue(20.0)
            .setMin(1.0)
            .setMax(100.0)
            .setSaveConsumer(Config.BASE_HEALTH::set)
            .build());
    }

    private void addMessageEntries(ConfigCategory category, ConfigEntryBuilder entries) {
        category.addEntry(entries.startStrField(Component.literal("触发增益提示"), Config.BUFF_MESSAGE.get())
            .setDefaultValue("§a感觉你的力量恢复了一些")
            .setTooltip(Component.literal("支持 Minecraft 颜色代码。"))
            .setSaveConsumer(Config.BUFF_MESSAGE::set)
            .build());
        category.addEntry(entries.startBooleanToggle(Component.literal("在物品栏上方显示提示"), Config.SHOW_MESSAGE_ABOVE_HOTBAR.get())
            .setDefaultValue(true)
            .setSaveConsumer(Config.SHOW_MESSAGE_ABOVE_HOTBAR::set)
            .build());
    }

    private void buildFoodNameMaps() {
        List<ResourceLocation> foodIds = BuiltInRegistries.ITEM.stream()
            .filter(item -> {
                ItemStack stack = new ItemStack(item);
                FoodProperties properties = stack.getFoodProperties(null);
                return properties != null;
            })
            .map(BuiltInRegistries.ITEM::getKey)
            .filter(id -> id != null)
            .sorted(Comparator.comparing(ResourceLocation::toString))
            .collect(Collectors.toList());

        Map<String, Integer> nameCounts = new HashMap<>();
        for (ResourceLocation id : foodIds) {
            nameCounts.merge(getItemName(id), 1, Integer::sum);
        }

        for (ResourceLocation id : foodIds) {
            String name = getItemName(id);
            String display = nameCounts.getOrDefault(name, 0) > 1 ? name + " [" + id.getNamespace() + "]" : name;
            displayToId.put(display, id.toString());
            idToDisplay.put(id.toString().toLowerCase(Locale.ROOT), display);
        }
    }

    private List<String> allFoodDisplays() {
        List<String> values = new ArrayList<>(displayToId.keySet());
        values.sort(String::compareTo);
        return values;
    }

    private AbstractConfigListEntry<?> createFoodDropdown(String label, List<String> selections, java.util.function.Consumer<String> saveConsumer) {
        List<String> values = new ArrayList<>();
        values.addAll(selections);
        return new AddFoodDropdownEntry(Component.literal(label), NO_SELECTION, values, saveConsumer);
    }

    private AbstractConfigListEntry<?> createPendingFoodList(String title, boolean whitelist) {
        return new PendingFoodListEntry(
            Component.literal(title),
            () -> whitelist ? pendingWhitelist : pendingBlacklist,
            () -> whitelist ? originalWhitelist : originalBlacklist,
            idToDisplay,
            id -> removeFoodFromList(id, whitelist),
            this::hasPendingListChanges
        );
    }

    private void addDisplayToList(String display, boolean whitelist) {
        String id = displayToId.get(display);
        if (id == null) {
            ResourceLocation parsed = ResourceLocation.tryParse(display);
            if (parsed != null) {
                id = parsed.toString();
            }
        }
        if (id == null) {
            return;
        }
        List<String> values = whitelist ? pendingWhitelist : pendingBlacklist;
        String normalized = id.toLowerCase(Locale.ROOT);
        if (!values.contains(normalized)) {
            values.add(normalized);
            values.sort(String::compareTo);
        }
    }

    private void removeFoodFromList(String id, boolean whitelist) {
        String target = id.toLowerCase(Locale.ROOT);
        List<String> values = whitelist ? pendingWhitelist : pendingBlacklist;
        values.removeIf(value -> value.toLowerCase(Locale.ROOT).equals(target));
    }

    private void initPendingLists() {
        if (pendingWhitelist == null) {
            pendingWhitelist = normalizeIds(Config.FOOD_WHITELIST.get());
            originalWhitelist = new ArrayList<>(pendingWhitelist);
        }
        if (pendingBlacklist == null) {
            pendingBlacklist = normalizeIds(Config.FOOD_BLACKLIST.get());
            originalBlacklist = new ArrayList<>(pendingBlacklist);
        }
    }

    private boolean hasPendingListChanges() {
        return !normalizeIds(pendingWhitelist).equals(normalizeIds(originalWhitelist))
            || !normalizeIds(pendingBlacklist).equals(normalizeIds(originalBlacklist));
    }

    private List<String> normalizeIds(List<? extends String> ids) {
        return ids.stream()
            .map(id -> id.toLowerCase(Locale.ROOT))
            .distinct()
            .sorted()
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private void savePendingLists() {
        Config.FOOD_WHITELIST.set(normalizeIds(pendingWhitelist));
        Config.FOOD_BLACKLIST.set(normalizeIds(pendingBlacklist));
        Config.sync();
        originalWhitelist = normalizeIds(pendingWhitelist);
        originalBlacklist = normalizeIds(pendingBlacklist);
    }

    private static void hideTopSearchBox(Screen screen) {
        hideSearchEditBox(screen);
        for (GuiEventListener child : screen.children()) {
            if (child instanceof EditBox editBox && isTopSearchBox(editBox)) {
                hideEditBox(editBox);
            }
        }
    }

    private static void hideSearchEditBox(Object target) {
        Class<?> type = target.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(target);
                    if (value instanceof EditBox editBox && isTopSearchBox(editBox)) {
                        hideEditBox(editBox);
                    } else if (value instanceof Collection<?> collection) {
                        hideSearchEditBoxes(collection);
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                }
            }
            type = type.getSuperclass();
        }
    }

    private static void hideSearchEditBoxes(Collection<?> collection) {
        for (Object value : collection) {
            if (value instanceof EditBox editBox && isTopSearchBox(editBox)) {
                hideEditBox(editBox);
            }
        }
        try {
            collection.removeIf(value -> value instanceof EditBox editBox && isTopSearchBox(editBox));
        } catch (UnsupportedOperationException ignored) {
        }
    }

    private static boolean isTopSearchBox(EditBox editBox) {
        String message = editBox.getMessage().getString().toLowerCase(Locale.ROOT);
        return message.contains("search") || (editBox.getY() < 80 && editBox.getWidth() > 200);
    }

    private static void hideEditBox(EditBox editBox) {
        editBox.visible = false;
        editBox.active = false;
        editBox.setFocused(false);
    }

    private String getItemName(ResourceLocation id) {
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
        return item.map(value -> new ItemStack(value).getHoverName().getString()).orElse(id.toString());
    }
}
