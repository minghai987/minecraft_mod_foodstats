package com.ming_hai.foodstats.client.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RemoteServerConfigBlockedScreen extends Screen {
    private final Screen parent;

    public RemoteServerConfigBlockedScreen(Screen parent) {
        super(Component.literal("食物增强配置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("返回"), button -> minecraft.setScreen(parent))
            .bounds(width / 2 - 50, height / 2 + 45, 100, 20)
            .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, "食物增强配置", width / 2, height / 2 - 45, 0xFFFFFF);
        guiGraphics.drawCenteredString(font, "当前正在连接远程服务器，客户端配置界面已禁用。", width / 2, height / 2 - 15, 0xFFFF55);
        guiGraphics.drawCenteredString(font, "服务器参数只能由管理员使用 /foodstats 指令修改。", width / 2, height / 2 + 5, 0xAAAAAA);
    }
}
