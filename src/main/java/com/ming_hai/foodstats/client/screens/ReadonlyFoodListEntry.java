package com.ming_hai.foodstats.client.screens;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ReadonlyFoodListEntry extends TooltipListEntry<String> {
    private final String value;
    private final Button deleteButton;
    private boolean pendingDelete;

    public ReadonlyFoodListEntry(String value, Runnable deleteAction) {
        super(Component.literal(value), () -> Optional.empty(), false);
        this.value = value;
        this.deleteButton = Button.builder(Component.literal("删除"), button -> {
                deleteAction.run();
                pendingDelete = true;
                button.setMessage(Component.literal("待删除"));
                button.active = false;
            })
            .bounds(0, 0, Minecraft.getInstance().font.width("删除") + 12, 20)
            .build();
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public Optional<String> getDefaultValue() {
        return Optional.of(value);
    }

    @Override
    public boolean isEdited() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, delta);
        int buttonWidth = deleteButton.getWidth();
        deleteButton.setX(x + entryWidth - buttonWidth);
        deleteButton.setY(y);
        graphics.drawString(Minecraft.getInstance().font, pendingDelete ? "待删除: " + value : value, x, y + 6, pendingDelete ? 0xAAAAAA : 0xFFFFFF, false);
        deleteButton.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return Collections.singletonList(deleteButton);
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return Collections.singletonList(deleteButton);
    }
}
