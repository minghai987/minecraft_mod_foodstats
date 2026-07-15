package com.ming_hai.foodstats.client.screens;

import com.ming_hai.foodstats.FoodStatsMod;
import com.ming_hai.foodstats.capability.IPlayerStats;
import com.ming_hai.foodstats.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import com.ming_hai.foodstats.compat.JEIIntegration;
import com.ming_hai.foodstats.compat.TavernDrinkCompat;

@SuppressWarnings("unchecked")
public class FoodGuideScreen extends Screen {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FoodStatsMod.MODID, "textures/gui/food_guide.png");
    private final IPlayerStats stats;
    private int currentPage = 0;
    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 220;
    private static final int ITEMS_PER_PAGE = 18;
    private List<ResourceLocation> allFoods;
    private List<ResourceLocation> eatenFoods;
    private List<ResourceLocation> uneatenFoods;
    private int eatenPages;
    private long lastConfigRevision = -1L;
    private int guiLeft;
    private int guiTop;
    private static final int COLUMNS = 3;
    private static final int ROWS = 6;
    private static final int ITEM_WIDTH = 80;
    private static final int ITEM_HEIGHT = 18;
    private static final int ITEM_SPACING_X = 85;
    private static final int ITEM_SPACING_Y = 20;
    private static final int BUTTON_Y = GUI_HEIGHT - 25;

    // 交互状态
    private int hoveredFoodIndex = -1;
    private boolean hoveredEatenButton = false;
    private boolean hoveredUneatenButton = false;
    private TabButton eatenTabButton;
    private TabButton uneatenTabButton;

    public FoodGuideScreen(IPlayerStats stats) {
        super(Component.literal("§6食物图鉴"));
        this.stats = stats;
    }

   private void initFoodLists() {
    // 获取所有可记录物品：普通食物 + 酒馆瓶装酒
    allFoods = BuiltInRegistries.ITEM.stream()
        .filter(item -> item != null)
        .filter(TavernDrinkCompat::isGuideListedItem)
        .map(BuiltInRegistries.ITEM::getKey)
        .filter(foodId -> foodId != null)
        .filter(foodId -> Config.isFoodAllowed(foodId.toString()))
        .sorted(Comparator.comparing(ResourceLocation::toString))
        .collect(Collectors.toList());

    eatenFoods = stats.getEatenFoods().stream()
        .filter(food -> Config.isFoodAllowed(food.toString()))
        .sorted(Comparator.comparing(ResourceLocation::toString))
        .collect(Collectors.toList());

    // 获取未吃食物列表
    uneatenFoods = allFoods.stream()
        .filter(food -> !stats.getEatenFoods().contains(food))
        .collect(Collectors.toList());

    eatenPages = (int) Math.ceil((double) eatenFoods.size() / ITEMS_PER_PAGE);
    lastConfigRevision = Config.getRevision();
    clampCurrentPage();
}
    
    @Override
    protected void init() {
        super.init();
        initFoodLists();
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;

        // 增加按钮大小并调整位置
        addRenderableWidget(new PageButton(
            guiLeft + 10, 
            guiTop + GUI_HEIGHT - 35,
            40,
            30,
            Component.literal("◀"), 
            button -> prevPage()
        ));

        addRenderableWidget(new PageButton(
            guiLeft + GUI_WIDTH - 50,
            guiTop + GUI_HEIGHT - 35,
            40,
            30,
            Component.literal("▶"), 
            button -> nextPage()
        ));

        eatenTabButton = new TabButton(guiLeft + 45, guiTop + 30, 90, 25,
                getEatenTabLabel(), button -> jumpToEaten());
        addRenderableWidget(eatenTabButton);

        uneatenTabButton = new TabButton(guiLeft + 145, guiTop + 30, 90, 25,
                getUneatenTabLabel(), button -> jumpToUneaten());
        addRenderableWidget(uneatenTabButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        refreshFoodListsIfConfigChanged();
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);

        int baseX = guiLeft + 28;
        int baseY = guiTop + 50;

        drawPageTitle(guiGraphics);
        updateHoverStates(mouseX, mouseY);

        if (currentPage == 0) {
            renderStatsPage(guiGraphics, baseX, baseY);
        } else {
            renderFoodListPage(guiGraphics, baseX, baseY, mouseX, mouseY);
        }
    }

    private void drawPageTitle(GuiGraphics guiGraphics) {
        if (currentPage == 0) return;
        
        String title = isShowingEaten() ? "§a已食用食物" : "§c未食用食物";
        int titleWidth = font.width(title);
        guiGraphics.drawString(
            font,
            title,
            guiLeft + (GUI_WIDTH - titleWidth) / 2,
            guiTop + 20,
            0xFFFFFF,
            false
        );
    }

    private void renderStatsPage(GuiGraphics guiGraphics, int baseX, int baseY) {
        // 获取当前加成值和配置上限
        int healthBonus = stats.getHealthBonus();
        int armorBonus = stats.getArmorBonus();
        int attackBonus = stats.getAttackBonus();
        int healthMax = Config.getHealthMax();
        int armorMax = Config.getArmorMax();
        int attackMax = Config.getAttackMax();
        
        int column1X = baseX;
        int column2X = baseX + 120;

        String healthText = healthMax > 0 ? 
                "血量: +" + String.format("%.1f", Config.getHealthBonusValue() * healthBonus) + 
                "/" + String.format("%.1f", Config.getHealthBonusValue() * healthMax) :
                "血量: +" + String.format("%.1f", Config.getHealthBonusValue() * healthBonus);
            
        String armorText = armorMax > 0 ? 
                "护甲: +" + String.format("%.1f", Config.getArmorBonusValue() * armorBonus) + 
                "/" + String.format("%.1f", Config.getArmorBonusValue() * armorMax) :
                "护甲: +" + String.format("%.1f", Config.getArmorBonusValue() * armorBonus);
            
        String attackText = attackMax > 0 ? 
                "攻击: +" + String.format("%.1f", Config.getAttackBonusValue() * attackBonus) + 
                "/" + String.format("%.1f", Config.getAttackBonusValue() * attackMax) :
                "攻击: +" + String.format("%.1f", Config.getAttackBonusValue() * attackBonus);

        guiGraphics.drawString(font, "增益方式: " + Config.getBonusMode().getDisplayName(), baseX, baseY, 0xFFFFFF, false);
        guiGraphics.drawString(font, "触发加成次数: " + stats.getBuffCount(), baseX, baseY + 20, 0xFFFFFF, false);
        guiGraphics.drawString(font, "已记录食物数量: " + eatenFoods.size(), baseX, baseY + 40, 0xFFFFFF, false);
        guiGraphics.drawString(font, "当前加成数值：", baseX, baseY + 60, 0xFFFF00, false);
        
        // 绘制加成数值（两列布局）
        guiGraphics.drawString(font, healthText, column1X, baseY + 70, 0xFF5555, false);
        guiGraphics.drawString(font, armorText, column1X, baseY + 90, 0x5555FF, false);
        guiGraphics.drawString(font, attackText, column2X, baseY + 70, 0xFF00FF, false);
        
        if (Config.getBonusMode() == Config.BonusMode.SATURATION_THRESHOLD) {
            int baseThreshold = Config.getThreshold();
            int thresholdIncrease = Config.getThresholdIncrease();
            int currentThreshold = baseThreshold + stats.getBuffCount() * thresholdIncrease;
            guiGraphics.drawString(font, "当前阶段饱食度: " + stats.getCurrentSaturation() + "/" + currentThreshold, baseX, baseY + 110, 0x55FF55, false);
        } else if (Config.getBonusMode() == Config.BonusMode.UNIQUE_FOOD_COUNT_THRESHOLD) {
            guiGraphics.drawString(font, "新食物累计: " + stats.getUniqueFoodProgress() + "/" + Config.getUniqueFoodsPerBonus(), baseX, baseY + 110, 0x55FF55, false);
        } else {
            guiGraphics.drawString(font, "下一种新的允许食物会触发增益", baseX, baseY + 110, 0x55FF55, false);
        }
    }

    private void renderFoodListPage(GuiGraphics guiGraphics, int baseX, int baseY, int mouseX, int mouseY) {
        int renderBaseY = guiTop + 50;
        List<ResourceLocation> currentList = getCurrentFoodList();
        int startIndex = getCurrentListPageIndex() * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int absoluteIndex = startIndex + i;
            if (absoluteIndex >= currentList.size()) break;

            int column = i % COLUMNS;
            int row = i / COLUMNS;
            int x = baseX + column * ITEM_SPACING_X;
            int y = baseY + row * ITEM_SPACING_Y;

            ResourceLocation foodId = currentList.get(absoluteIndex);
            boolean isHovered = (hoveredFoodIndex == absoluteIndex);
            renderFoodItem(guiGraphics, x, y, foodId, isHovered);
        }
    }

    private void renderFoodItem(GuiGraphics guiGraphics, int x, int y, ResourceLocation foodId, boolean isHovered) {
        if (foodId == null) return;
        Item item = BuiltInRegistries.ITEM.get(foodId);
        if (item == null) return;

        if (isHovered) {
            guiGraphics.fill(x - 2, y - 2, x + ITEM_WIDTH, y + ITEM_HEIGHT, 0x80FFFFFF);
        }

        guiGraphics.renderItem(new ItemStack(item), x, y);
        guiGraphics.renderItemDecorations(font, new ItemStack(item), x, y);

        String name = item.getDescription().getString();
        if (name.length() > 10) {
            name = name.substring(0, 10) + "...";
        }
        
        Component displayName = Component.literal(name).withStyle(style -> style.withUnderlined(isHovered));
        guiGraphics.drawString(font, displayName, x + 20, y + 4, 0xFFFFFF, false);
    }

    private void updateHoverStates(int mouseX, int mouseY) {
        hoveredFoodIndex = -1;
        hoveredEatenButton = false;
        hoveredUneatenButton = false;

        if (currentPage > 0) {
            List<ResourceLocation> currentList = getCurrentFoodList();
            int startIndex = getCurrentListPageIndex() * ITEMS_PER_PAGE;
            
            for (int i = 0; i < ITEMS_PER_PAGE; i++) {
                int absoluteIndex = startIndex + i;
                if (absoluteIndex >= currentList.size()) break;

                int column = i % COLUMNS;
                int row = i / COLUMNS;
                int x = guiLeft + 28 + column * ITEM_SPACING_X;
                int y = guiTop + 50 + row * ITEM_SPACING_Y;

                if (isMouseOver(mouseX, mouseY, x, y, ITEM_WIDTH, ITEM_HEIGHT)) {
                    hoveredFoodIndex = absoluteIndex;
                    break;
                }
            }
        }
    }

    private boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    // Button classes
    private class PageButton extends Button {
        public PageButton(int x, int y, int width, int height, Component text, OnPress onPress) {
            super(x, y, width, height, text, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int bgColor = isHovered() ? 0x80FFFFFF : 0x40000000;
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
            
            int borderColor = isHovered() ? 0xFFFFFFFF : 0xFFAAAAAA;
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + 1, borderColor);
            guiGraphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor);
            guiGraphics.fill(getX(), getY(), getX() + 1, getY() + height, borderColor);
            guiGraphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor);
            
            int textColor = isHovered() ? 0xFFFF00 : 0xFFFFFF;
            guiGraphics.drawCenteredString(font, getMessage(), getX() + width/2, getY() + (height-8)/2, textColor);
        }
    }

    private class TabButton extends Button {
        public TabButton(int x, int y, int width, int height, Component text, OnPress onPress) {
            super(x, y, width, height, text, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int color = isHovered() ? 0xFFFFA0 : 0xFFFFFF;
            if (isHovered()) guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0x60FFFFFF);
            guiGraphics.drawCenteredString(font, getMessage().copy().withStyle(style -> style.withUnderlined(isHovered())), 
                    getX() + width/2, getY() + (height-8)/2, color);
        }
    }

    private void jumpToEaten() { currentPage = 1; }
    private void jumpToUneaten() { currentPage = getFirstUneatenPage(); }
    
    private void nextPage() {
        if (currentPage < getTotalPages()) {
            currentPage++;
            hoveredFoodIndex = -1;
        }
    }

    private void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            hoveredFoodIndex = -1;
        }
    }
    
    private int getTotalPages() {
        return eatenPages + (int) Math.ceil((double) uneatenFoods.size() / ITEMS_PER_PAGE);
    }

    private void refreshFoodListsIfConfigChanged() {
        if (lastConfigRevision != Config.getRevision()) {
            initFoodLists();
            updateTabButtonLabels();
            hoveredFoodIndex = -1;
        }
    }

    private void clampCurrentPage() {
        int totalPages = getTotalPages();
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        if (currentPage < 0) {
            currentPage = 0;
        }
    }
    
    private boolean isShowingEaten() {
        return currentPage > 0 && currentPage <= eatenPages;
    }
    
    private List<ResourceLocation> getCurrentFoodList() {
        return isShowingEaten() ? eatenFoods : uneatenFoods;
    }

    private Component getEatenTabLabel() {
        return Component.literal("已食用（" + eatenFoods.size() + "）");
    }

    private Component getUneatenTabLabel() {
        return Component.literal("未食用（" + uneatenFoods.size() + "）");
    }

    private void updateTabButtonLabels() {
        if (eatenTabButton != null) {
            eatenTabButton.setMessage(getEatenTabLabel());
        }
        if (uneatenTabButton != null) {
            uneatenTabButton.setMessage(getUneatenTabLabel());
        }
    }

    private int getCurrentListPageIndex() {
        if (currentPage <= 0) {
            return 0;
        }
        if (isShowingEaten()) {
            return currentPage - 1;
        }
        return currentPage - getFirstUneatenPage();
    }

    private int getFirstUneatenPage() {
        return eatenPages + 1;
    }

   @Override
public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (button == 1 && currentPage > 0) {  // 右键
        int baseX = guiLeft + 28;
        int baseY = guiTop + 50;
        List<ResourceLocation> currentList = getCurrentFoodList();
        int startIndex = getCurrentListPageIndex() * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int absoluteIndex = startIndex + i;
            
            if (absoluteIndex < 0 || absoluteIndex >= currentList.size()) {
                continue;
            }

            int column = i % COLUMNS;
            int row = i / COLUMNS;
            int x = baseX + column * ITEM_SPACING_X;
            int y = baseY + row * ITEM_SPACING_Y;

            if (isMouseOver((int)mouseX, (int)mouseY, x, y, ITEM_WIDTH, ITEM_HEIGHT)) {
                ResourceLocation foodId = currentList.get(absoluteIndex);
                Item item = BuiltInRegistries.ITEM.get(foodId);
                if (item != null) {
                    ItemStack stack = new ItemStack(item);
                    
                    // 尝试显示配方
                    boolean success = com.ming_hai.foodstats.compat.JEIIntegration.showRecipes(stack);
                    
                    if (success) {
                        // 成功显示配方，可以选择是否关闭界面
                        // minecraft.setScreen(null); // 可以选择关闭界面
                        return true;
                    }
                }
            }
        }
    }
    return super.mouseClicked(mouseX, mouseY, button);
}


}