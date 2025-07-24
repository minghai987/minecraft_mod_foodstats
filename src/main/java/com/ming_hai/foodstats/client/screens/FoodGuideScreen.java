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
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.List;
import net.minecraft.world.entity.player.Player;


public class FoodGuideScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation(FoodStatsMod.MODID, "textures/gui/food_guide.png");
    private final IPlayerStats stats;
    private int currentPage = 0;
    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 220;
    private static final int ITEMS_PER_PAGE = 20;
    private List<ResourceLocation> allFoods;
    private List<ResourceLocation> uneatenFoods;
    private int eatenPages;
    private int guiLeft;
    private int guiTop;
    private static final int COLUMNS = 3; // 4列布局
    private static final int ROWS = 6;
    // 调整位置参数
    private static final int ITEM_WIDTH = 80;
    private static final int ITEM_HEIGHT = 18;
    private static final int ITEM_SPACING_X = 85; // 水平间距
    private static final int ITEM_SPACING_Y = 20; // 垂直间距
    private static final int BUTTON_Y = GUI_HEIGHT - 25;


    // 交互状态
    private int hoveredFoodIndex = -1;
    private boolean hoveredEatenButton = false;
    private boolean hoveredUneatenButton = false;

    public FoodGuideScreen(IPlayerStats stats) {
        super(Component.literal("§6食物图鉴"));
        this.stats = stats;
        // 提前初始化列表
        initFoodLists();
    }

   private void initFoodLists() {
    List<? extends String> blacklist = Config.FOOD_BLACKLIST.get();

    // 确保 ForgeRegistries 已初始化
    if (ForgeRegistries.ITEMS == null) {
        FoodStatsMod.LOGGER.error("Item registry not initialized!");
        allFoods = new ArrayList<>();
        uneatenFoods = new ArrayList<>();
        return;
    }
    
    // 确保获取所有食物（包括模组添加的）
    allFoods = ForgeRegistries.ITEMS.getValues().stream()
        .filter(Objects::nonNull)
        .filter(item -> item.getFoodProperties() != null)
        .map(ForgeRegistries.ITEMS::getKey)
        .filter(Objects::nonNull)
        .filter(foodId -> !blacklist.contains(foodId.toString())) // 应用黑名单
        .sorted(Comparator.comparing(ResourceLocation::toString))
        .collect(Collectors.toList());

    // 确保未吃食物列表完整
    uneatenFoods = allFoods.stream()
        .filter(food -> !stats.getEatenFoods().contains(food))
        .collect(Collectors.toList());
}
    

    @Override
    protected void init() {
        super.init();
        initFoodLists(); // 在init中初始化列表
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;

        eatenPages = (int) Math.ceil((double) stats.getEatenFoods().size() / ITEMS_PER_PAGE);

        // 增加按钮大小并调整位置
addRenderableWidget(new PageButton(
    guiLeft + 10, 
    guiTop + GUI_HEIGHT - 35, // 上移5像素
    40, // 宽度从30增加到40
    30, // 高度从20增加到30
    Component.literal("◀"), 
    button -> prevPage()
));

addRenderableWidget(new PageButton(
    guiLeft + GUI_WIDTH - 50, // 调整位置适应新宽度
    guiTop + GUI_HEIGHT - 35, // 上移5像素
    40, // 宽度从30增加到40
    30, // 高度从20增加到30
    Component.literal("▶"), 
    button -> nextPage()
));

        addRenderableWidget(new TabButton(guiLeft + 50, guiTop + 30, 70, 25, 
                Component.literal("已食用"), button -> jumpToEaten()));
        
        addRenderableWidget(new TabButton(guiLeft + 150, guiTop + 30, 70, 25, 
                Component.literal("未食用"), button -> jumpToUneaten()));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
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
    int healthMax = Config.HEALTH_MAX.get();
    int armorMax = Config.ARMOR_MAX.get();
    int attackMax = Config.ATTACK_MAX.get();
    
    // 获取阈值信息
    int baseThreshold = Config.THRESHOLD.get();
    int thresholdIncrease = Config.THRESHOLD_INCREASE.get();


   int currentSaturation = stats.getCurrentSaturation();
    // 计算当前阈值（基础阈值 + 已获得加成次数 × 阈值增量）
    int currentThreshold = baseThreshold + stats.getBuffCount() * thresholdIncrease;
    
    // 使用正确的变量生成文本
    int column1X = baseX;
    int column2X = baseX + 120;

    String healthText = healthMax > 0 ? 
            "血量: +" + String.format("%.1f", Config.HEALTH_BONUS.get() * healthBonus) + 
            "/" + String.format("%.1f", Config.HEALTH_BONUS.get() * healthMax) :
            "血量: +" + String.format("%.1f", Config.HEALTH_BONUS.get() * healthBonus);
        
        String armorText = armorMax > 0 ? 
            "护甲: +" + String.format("%.1f", Config.ARMOR_BONUS.get() * armorBonus) + 
            "/" + String.format("%.1f", Config.ARMOR_BONUS.get() * armorMax) :
            "护甲: +" + String.format("%.1f", Config.ARMOR_BONUS.get() * armorBonus);
        
        String attackText = attackMax > 0 ? 
            "攻击: +" + String.format("%.1f", Config.ATTACK_BONUS.get() * attackBonus) + 
            "/" + String.format("%.1f", Config.ATTACK_BONUS.get() * attackMax) :
            "攻击: +" + String.format("%.1f", Config.ATTACK_BONUS.get() * attackBonus);


    // 绘制基本信息
    guiGraphics.drawString(font, "当前阶段饱食度: " + currentSaturation + "/" + currentThreshold, baseX, baseY, 0xFFFFFF, false);
    guiGraphics.drawString(font, "触发加成次数: " + stats.getBuffCount(), baseX, baseY + 20, 0xFFFFFF, false);
    guiGraphics.drawString(font, "已食用食物数量: " + stats.getEatenFoods().size(), baseX, baseY + 40, 0xFFFFFF, false);
    guiGraphics.drawString(font, "当前加成数值：", baseX, baseY + 60, 0xFFFF00, false);
    
    // 绘制加成数值（两列布局）
    guiGraphics.drawString(font, healthText, column1X, baseY + 70, 0xFF5555, false);
    guiGraphics.drawString(font, armorText, column1X, baseY + 90, 0x5555FF, false);
    guiGraphics.drawString(font, attackText, column2X, baseY + 70, 0xFF00FF, false);
    
    // 显示阈值增加信息
    if (thresholdIncrease > 0) {
        String thresholdInfo = "当前阈值: " + currentThreshold + " (基础: " + baseThreshold + " + 增量: " + thresholdIncrease + " × 加成次数)";
        guiGraphics.drawString(font, thresholdInfo, baseX, baseY + 110, 0x55FF55, false);
    }
}


    private void renderFoodListPage(GuiGraphics guiGraphics, int baseX, int baseY, int mouseX, int mouseY) {
        // 使用相同的基准坐标
        int renderBaseY = guiTop + 50;
        List<ResourceLocation> currentList = getCurrentFoodList();
        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int absoluteIndex = startIndex + i;
            if (absoluteIndex >= currentList.size()) break;

            // 使用网格布局计算位置
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
        Item item = ForgeRegistries.ITEMS.getValue(foodId);
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

                int column = i % COLUMNS;
                int row = i / COLUMNS;
                int x = guiLeft + 28 + column * ITEM_SPACING_X;
                int y = guiTop + 50 + row * ITEM_SPACING_Y;

                // 使用精确的点击区域检测
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
        // 添加按钮背景
        int bgColor = isHovered ? 0x80FFFFFF : 0x40000000; // 悬停时半透明白色，否则半透明黑色
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
        
        // 添加按钮边框
        int borderColor = isHovered ? 0xFFFFFFFF : 0xFFAAAAAA; // 悬停时白色边框，否则灰色
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + 1, borderColor); // 上边框
        guiGraphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor); // 下边框
        guiGraphics.fill(getX(), getY(), getX() + 1, getY() + height, borderColor); // 左边框
        guiGraphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor); // 右边框
        
        // 文本颜色
        int textColor = isHovered ? 0xFFFF00 : 0xFFFFFF; // 悬停时黄色，否则白色
        guiGraphics.drawCenteredString(font, getMessage(), getX() + width/2, getY() + (height-8)/2, textColor);
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
  private void nextPage() {
    if (currentPage < getTotalPages()) {
        currentPage++;
        // 重置悬停状态
        hoveredFoodIndex = -1;
    }
}

private void prevPage() {
    if (currentPage > 0) {
        currentPage--;
        // 重置悬停状态
        hoveredFoodIndex = -1;
    }
}

    
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
        // 只处理右键点击且当前页面是食物列表页(非0页)
        if (button == 1 && currentPage > 0) {
            int baseX = guiLeft + 28;
            int baseY = guiTop + 50;  // 与渲染位置一致
            List<ResourceLocation> currentList = getCurrentFoodList();
            int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;

            for (int i = 0; i < ITEMS_PER_PAGE; i++) {
                int absoluteIndex = startIndex + i;
                
                // 检查索引是否有效
                if (absoluteIndex < 0 || absoluteIndex >= currentList.size()) {
                    continue;
                }

                int column = i % COLUMNS;
                int row = i / COLUMNS;
                int x = baseX + column * ITEM_SPACING_X;
                int y = baseY + row * ITEM_SPACING_Y;

                if (isMouseOver((int)mouseX, (int)mouseY, x, y, ITEM_WIDTH, ITEM_HEIGHT)) {
                    ResourceLocation foodId = currentList.get(absoluteIndex);
                    Item item = ForgeRegistries.ITEMS.getValue(foodId);
                    if (item != null) {
                        showRecipes(new ItemStack(item));
                    }
                    return true; // 处理了点击事件
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

   private void showRecipes(ItemStack stack) {
    if (!FoodStatsMod.JEI_LOADED || FoodStatsMod.jeiRuntime == null) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal("JEI 未安装或未加载"), false);
        }
        return;
    }

    try {
        // 动态获取 JEI 运行时和类
        Object jeiRuntime = FoodStatsMod.jeiRuntime;
        Class<?> runtimeClass = Class.forName("mezz.jei.api.runtime.IJeiRuntime");
        Class<?> focusFactoryClass = Class.forName("mezz.jei.api.recipe.IFocusFactory");
        Class<?> ingredientTypeClass = Class.forName("mezz.jei.api.ingredients.IIngredientType");
        Class<?> vanillaTypesClass = Class.forName("mezz.jei.api.constants.VanillaTypes");
        Class<?> roleClass = Class.forName("mezz.jei.api.recipe.RecipeIngredientRole");

        // 动态调用 JEI API
        Object focusFactory = runtimeClass.getMethod("getJeiHelpers").invoke(jeiRuntime);
        focusFactory = focusFactoryClass.getMethod("getFocusFactory").invoke(focusFactory);
        
        Object itemStackType = vanillaTypesClass.getField("ITEM_STACK").get(null);
        Object roleOutput = Enum.valueOf((Class<Enum>) roleClass, "OUTPUT");
        
        Object focus = focusFactoryClass.getMethod("createFocus", roleClass, ingredientTypeClass, Object.class)
                .invoke(focusFactory, roleOutput, itemStackType, stack);
        
        Object recipesGui = runtimeClass.getMethod("getRecipesGui").invoke(jeiRuntime);
        recipesGui.getClass().getMethod("show", focus.getClass()).invoke(recipesGui, focus);
    } catch (Exception e) {
        FoodStatsMod.LOGGER.error("JEI 配方显示错误: ", e);
    }
}
}




    
    
