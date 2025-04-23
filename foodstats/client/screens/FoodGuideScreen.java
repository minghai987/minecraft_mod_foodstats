package com.ming_hai.foodstats.client.screens;

import com.ming_hai.foodstats.FoodStatsMod;
import com.ming_hai.foodstats.capability.IPlayerStats;
import com.ming_hai.foodstats.config.Config;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FoodGuideScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation(FoodStatsMod.MODID, "textures/gui/food_guide.png");
    private final IPlayerStats stats;
    private int currentPage = 0;
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 192;
    private static final int ITEMS_PER_PAGE = 12;
    private List<ResourceLocation> allFoods;
    private List<ResourceLocation> uneatenFoods;
    private int eatenPages;
    private int guiLeft;
    private int guiTop;

    // 交互状态
    private int hoveredFoodIndex = -1;
    private boolean hoveredEatenButton = false;
    private boolean hoveredUneatenButton = false;

    public FoodGuideScreen(IPlayerStats stats) {
        super(Component.literal("§6食物图鉴"));
        this.stats = stats;
        initFoodLists();
    }

    private void initFoodLists() {
        allFoods = ForgeRegistries.ITEMS.getValues().stream()
                .filter(item -> item.getFoodProperties() != null)
                .map(ForgeRegistries.ITEMS::getKey)
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .collect(Collectors.toList());

        uneatenFoods = allFoods.stream()
                .filter(food -> !stats.getEatenFoods().contains(food))
                .collect(Collectors.toList());
    }

    @Override
    protected void init() {
        super.init();
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;

        eatenPages = (int) Math.ceil((double) stats.getEatenFoods().size() / ITEMS_PER_PAGE);

        // 修改回调方法签名
        addRenderableWidget(new PageButton(guiLeft + 10, guiTop + GUI_HEIGHT - 30, 30, 20, 
                Component.literal("◀"), button -> prevPage()));
        
        addRenderableWidget(new PageButton(guiLeft + GUI_WIDTH - 40, guiTop + GUI_HEIGHT - 30, 30, 20, 
                Component.literal("▶"), button -> nextPage()));

        addRenderableWidget(new TabButton(guiLeft + 50, guiTop + 160, 60, 20, 
                Component.literal("已食用"), button -> jumpToEaten()));
        
        addRenderableWidget(new TabButton(guiLeft + 140, guiTop + 160, 60, 20, 
                Component.literal("未食用"), button -> jumpToUneaten()));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);

        int baseX = guiLeft + 28;
        int baseY = guiTop + 40;

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
        int currentSaturation = stats.getTotalSaturation() % Config.THRESHOLD.get();
        int totalBuffs = stats.getBuffCount();

        guiGraphics.drawString(font, "当前阶段饱食度: " + currentSaturation + "/" + Config.THRESHOLD.get(), baseX, baseY, 0xFFFFFF, false);
        guiGraphics.drawString(font, "触发加成次数: " + totalBuffs, baseX, baseY + 20, 0xFFFFFF, false);
        guiGraphics.drawString(font, "已食用食物数量: " + stats.getEatenFoods().size(), baseX, baseY + 40, 0xFFFFFF, false);
        guiGraphics.drawString(font, "当前加成数值：", baseX, baseY + 60, 0xFFFF00, false);
        guiGraphics.drawString(font, "血量: +" + Config.HEALTH_BONUS.get() * totalBuffs, baseX + 10, baseY + 80, 0xFF5555, false);
        guiGraphics.drawString(font, "护甲: +" + Config.ARMOR_BONUS.get() * totalBuffs, baseX + 10, baseY + 100, 0x5555FF, false);
        guiGraphics.drawString(font, "攻击: +" + Config.ATTACK_BONUS.get() * totalBuffs, baseX + 10, baseY + 120, 0xFFAA00, false);
    }

    private void renderFoodListPage(GuiGraphics guiGraphics, int baseX, int baseY, int mouseX, int mouseY) {
        List<ResourceLocation> currentList = getCurrentFoodList();
        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int absoluteIndex = startIndex + i;
            if (absoluteIndex >= currentList.size()) break;

            int column = i % 2;
            int row = i / 2;
            int x = baseX + column * 100;
            int y = baseY + row * 20;

            ResourceLocation foodId = currentList.get(absoluteIndex);
            boolean isHovered = absoluteIndex == hoveredFoodIndex;
            renderFoodItem(guiGraphics, x, y, foodId, isHovered);
        }
    }

    private void renderFoodItem(GuiGraphics guiGraphics, int x, int y, ResourceLocation foodId, boolean isHovered) {
        Item item = ForgeRegistries.ITEMS.getValue(foodId);
        if (item == null) return;

        if (isHovered) {
            guiGraphics.fill(x - 2, y - 2, x + 90, y + 18, 0x80FFFFFF);
        }

        guiGraphics.renderItem(new ItemStack(item), x, y);
        guiGraphics.renderItemDecorations(font, new ItemStack(item), x, y);
        
        Component name = item.getDescription().copy().withStyle(style -> style.withUnderlined(isHovered));
        guiGraphics.drawString(font, name, x + 20, y + 4, 0xFFFFFF, false);
    }

    private void updateHoverStates(int mouseX, int mouseY) {
        hoveredFoodIndex = -1;
        hoveredEatenButton = false;
        hoveredUneatenButton = false;

        // Check buttons
        if (isMouseOver(mouseX, mouseY, guiLeft + 50, guiTop + 160, 60, 20)) {
            hoveredEatenButton = true;
        } else if (isMouseOver(mouseX, mouseY, guiLeft + 140, guiTop + 160, 60, 20)) {
            hoveredUneatenButton = true;
        }

        // Check items
        if (currentPage > 0) {
            List<ResourceLocation> currentList = getCurrentFoodList();
            int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
            
            for (int i = 0; i < ITEMS_PER_PAGE; i++) {
                int absoluteIndex = startIndex + i;
                if (absoluteIndex >= currentList.size()) break;

                int column = i % 2;
                int row = i / 2;
                int x = guiLeft + 28 + column * 100;
                int y = guiTop + 40 + row * 20;

                if (isMouseOver(mouseX, mouseY, x, y, 90, 18)) {
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
            int color = isHovered ? 0xFF00FF00 : 0xFFFFFFFF;
            guiGraphics.drawCenteredString(font, getMessage(), getX() + width/2, getY() + (height-8)/2, color);
        }
    }

    private class TabButton extends Button {
        public TabButton(int x, int y, int width, int height, Component text, OnPress onPress) {
            super(x, y, width, height, text, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int color = isHovered ? 0xFFFFA0 : 0xFFFFFF;
            if (isHovered) guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0x60FFFFFF);
            guiGraphics.drawCenteredString(font, getMessage().copy().withStyle(style -> style.withUnderlined(isHovered)), 
                    getX() + width/2, getY() + (height-8)/2, color);
        }
    }

    private void jumpToEaten() { currentPage = 1; }
    private void jumpToUneaten() { currentPage = eatenPages + 1; }
    private void prevPage() { if (currentPage > 0) currentPage--; }
    private void nextPage() { if (currentPage < getTotalPages()) currentPage++; }

    
    private int getTotalPages() {
        return eatenPages + (int) Math.ceil((double) uneatenFoods.size() / ITEMS_PER_PAGE);
    }
    
    private boolean isShowingEaten() {
        return currentPage <= eatenPages;
    }
    
    private List<ResourceLocation> getCurrentFoodList() {
        return isShowingEaten() ? new ArrayList<>(stats.getEatenFoods()) : uneatenFoods;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            int baseX = guiLeft + 28;
            int baseY = guiTop + 40;
            List<ResourceLocation> currentList = getCurrentFoodList();
            int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;

            for (int i = 0; i < ITEMS_PER_PAGE; i++) {
                int absoluteIndex = startIndex + i;
                if (absoluteIndex >= currentList.size()) break;

                int column = i % 2;
                int row = i / 2;
                int x = baseX + column * 100;
                int y = baseY + row * 20;

                if (isMouseOver((int)mouseX, (int)mouseY, x, y, 90, 18)) {
                    ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(currentList.get(absoluteIndex)));
                    showRecipes(stack);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void showRecipes(ItemStack stack) {
        if (FoodStatsMod.JEI_LOADED) {
            try {
                IJeiRuntime jeiRuntime = (IJeiRuntime) FoodStatsMod.jeiRuntime;
                IFocusFactory focusFactory = jeiRuntime.getJeiHelpers().getFocusFactory();
                jeiRuntime.getRecipesGui().show(focusFactory.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack));
            } catch (Exception e) {
                Minecraft.getInstance().player.displayClientMessage(Component.literal("JEI错误: " + e.getMessage()), false);
            }
        } else {
            Minecraft.getInstance().player.displayClientMessage(Component.literal("请安装JEI查看配方"), false);
        }
    }
}




    
    
