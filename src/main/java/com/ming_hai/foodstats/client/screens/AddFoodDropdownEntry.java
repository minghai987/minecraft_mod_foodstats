package com.ming_hai.foodstats.client.screens;

import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class AddFoodDropdownEntry extends DropdownBoxEntry<String> {
    private int lastX;
    private int lastY;
    private int lastWidth;
    private static final int ENTRY_HEIGHT = 20;
    private final String emptySelection;
    private final List<String> selections;

    public AddFoodDropdownEntry(
        Component fieldName,
        String emptySelection,
        List<String> selections,
        Consumer<String> addConsumer
    ) {
        super(
            fieldName,
            Component.literal("添加"),
            () -> Optional.empty(),
            false,
            () -> "",
            null,
            selections,
            new PlaceholderTopCellElement(emptySelection),
            DropdownMenuBuilder.CellCreatorBuilder.of(Component::literal)
        );
        this.emptySelection = emptySelection;
        this.selections = selections;

        this.resetButton = Button.builder(
            Component.literal("添加"),
            button -> {
                String selected = resolveSelection(getValue());
                if (selected != null) {
                    addConsumer.accept(selected);
                    getSelectionElement().getTopRenderer().setValue("");
                    collapseSuggestions();
                }
            }
        ).bounds(0, 0, Minecraft.getInstance().font.width("添加") + 10, 20).build();
        setSuggestionMode(true);
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
        this.lastX = x;
        this.lastY = y;
        this.lastWidth = entryWidth;
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (!handled && !isInsideEntryRow(mouseX, mouseY)) {
            collapseSuggestions();
        }
        return handled;
    }

    @Override
    public void updateSelected(boolean selected) {
        super.updateSelected(selected);
        if (!selected) {
            collapseSuggestions();
        }
    }

    private boolean isInsideEntryRow(double mouseX, double mouseY) {
        return mouseX >= lastX && mouseX <= lastX + lastWidth && mouseY >= lastY && mouseY <= lastY + ENTRY_HEIGHT;
    }

    private void collapseSuggestions() {
        setFocused(null);
        getSelectionElement().setFocused(null);
    }

    private String resolveSelection(String value) {
        if (value == null || value.isBlank() || value.equals(emptySelection)) {
            return null;
        }
        for (String selection : selections) {
            if (selection.equals(value)) {
                return selection;
            }
        }
        String needle = value.toLowerCase();
        for (String selection : selections) {
            if (selection.toLowerCase().contains(needle)) {
                return selection;
            }
        }
        return value;
    }

    private static class PlaceholderTopCellElement extends DropdownBoxEntry.SelectionTopCellElement<String> {
        private final String placeholder;
        private final EditBox input;

        private PlaceholderTopCellElement(String placeholder) {
            this.placeholder = placeholder;
            this.input = new EditBox(Minecraft.getInstance().font, 0, 0, 148, 18, Component.empty());
            this.input.setBordered(false);
            this.input.setMaxLength(999999);
            this.input.setValue("");
        }

        @Override
        public String getValue() {
            return input.getValue();
        }

        @Override
        public void setValue(String value) {
            input.setValue(value == null ? "" : value);
            input.moveCursorTo(0, false);
        }

        @Override
        public Component getSearchTerm() {
            return Component.literal(input.getValue());
        }

        @Override
        public Optional<Component> getError() {
            return Optional.empty();
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height, float delta) {
            input.setX(x + 4);
            input.setY(y + 6);
            input.setWidth(width - 8);
            input.setEditable(getParent().isEditable());
            input.setTextColor(getPreferredTextColor());
            input.setFocused(isSuggestionMode() && isSelected && getParent().getFocused() == getParent().getSelectionElement() && getParent().getSelectionElement().getFocused() == this && getFocused() == input);
            input.render(graphics, mouseX, mouseY, delta);
            if (input.getValue().isEmpty() && !input.isFocused()) {
                graphics.drawString(Minecraft.getInstance().font, placeholder, x + 4, y + 6, 0x777777, false);
            }
        }

        @Override
        public List<EditBox> children() {
            return Collections.singletonList(input);
        }
    }
}
