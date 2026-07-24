package com.ming_hai.foodstats.client.screens;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PendingFoodListEntry extends TooltipListEntry<List<String>> {
    private static final int HEADER_HEIGHT = 24;
    private static final int ROW_HEIGHT = 24;

    private final Supplier<List<String>> valuesSupplier;
    private final Supplier<List<String>> defaultSupplier;
    private final Map<String, String> idToDisplay;
    private final Consumer<String> removeConsumer;
    private final BooleanSupplier editedSupplier;
    private final List<Button> deleteButtons = new ArrayList<>();
    private boolean expanded = false;
    private int lastX;
    private int lastY;
    private int lastWidth;

    public PendingFoodListEntry(
        Component title,
        Supplier<List<String>> valuesSupplier,
        Supplier<List<String>> defaultSupplier,
        Map<String, String> idToDisplay,
        Consumer<String> removeConsumer,
        BooleanSupplier editedSupplier
    ) {
        super(title, () -> Optional.empty(), false);
        this.valuesSupplier = valuesSupplier;
        this.defaultSupplier = defaultSupplier;
        this.idToDisplay = idToDisplay;
        this.removeConsumer = removeConsumer;
        this.editedSupplier = editedSupplier;
    }

    @Override
    public List<String> getValue() {
        return new ArrayList<>(valuesSupplier.get());
    }

    @Override
    public Optional<List<String>> getDefaultValue() {
        return Optional.of(new ArrayList<>(defaultSupplier.get()));
    }

    @Override
    public boolean isEdited() {
        return editedSupplier.getAsBoolean();
    }

    @Override
    public int getItemHeight() {
        int rows = expanded ? Math.max(1, valuesSupplier.get().size()) : 0;
        return HEADER_HEIGHT + rows * ROW_HEIGHT;
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, delta);
        this.lastX = x;
        this.lastY = y;
        this.lastWidth = entryWidth;
        List<String> values = valuesSupplier.get();
        graphics.drawString(Minecraft.getInstance().font, (expanded ? "[-] " : "[+] ") + getFieldName().getString() + " (" + values.size() + ")", x, y + 6, 0xFFFFFF, false);

        syncDeleteButtons(values);
        if (!expanded) {
            return;
        }
        if (values.isEmpty()) {
            graphics.drawString(Minecraft.getInstance().font, "（空）", x + 12, y + HEADER_HEIGHT + 6, 0xAAAAAA, false);
            return;
        }

        int rowY = y + HEADER_HEIGHT;
        for (int i = 0; i < values.size(); i++) {
            String id = values.get(i);
            Button deleteButton = deleteButtons.get(i);
            deleteButton.setX(x + entryWidth - deleteButton.getWidth());
            deleteButton.setY(rowY + 1);
            graphics.drawString(Minecraft.getInstance().font, idToDisplay.getOrDefault(id, id), x + 12, rowY + 6, 0xFFFFFF, false);
            deleteButton.render(graphics, mouseX, mouseY, delta);
            rowY += ROW_HEIGHT;
        }
    }

    @Override
    public List<? extends GuiEventListener> children() {
        List<GuiEventListener> children = new ArrayList<>();
        if (expanded) {
            syncDeleteButtons(valuesSupplier.get());
            children.addAll(deleteButtons);
        }
        return children;
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        List<NarratableEntry> narratables = new ArrayList<>();
        if (expanded) {
            syncDeleteButtons(valuesSupplier.get());
            narratables.addAll(deleteButtons);
        }
        return narratables;
    }

    private void syncDeleteButtons(List<String> values) {
        while (deleteButtons.size() > values.size()) {
            deleteButtons.remove(deleteButtons.size() - 1);
        }
        while (deleteButtons.size() < values.size()) {
            deleteButtons.add(createDeleteButton(deleteButtons.size()));
        }
    }

    private Button createDeleteButton(int index) {
        return Button.builder(Component.literal("删除"), button -> {
            List<String> values = valuesSupplier.get();
            if (index >= 0 && index < values.size()) {
                removeConsumer.accept(values.get(index));
            }
        }).bounds(0, 0, Minecraft.getInstance().font.width("删除") + 12, 20).build();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= lastX && mouseX <= lastX + lastWidth && mouseY >= lastY && mouseY <= lastY + HEADER_HEIGHT) {
            expanded = !expanded;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
