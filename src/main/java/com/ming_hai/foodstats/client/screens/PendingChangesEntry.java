package com.ming_hai.foodstats.client.screens;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

public class PendingChangesEntry extends TooltipListEntry<Boolean> {
    private final BooleanSupplier editedSupplier;

    public PendingChangesEntry(BooleanSupplier editedSupplier) {
        super(Component.empty(), () -> Optional.empty(), false);
        this.editedSupplier = editedSupplier;
    }

    @Override
    public Boolean getValue() {
        return editedSupplier.getAsBoolean();
    }

    @Override
    public Optional<Boolean> getDefaultValue() {
        return Optional.of(false);
    }

    @Override
    public boolean isEdited() {
        return editedSupplier.getAsBoolean();
    }

    @Override
    public int getItemHeight() {
        return 0;
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return Collections.emptyList();
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return Collections.emptyList();
    }
}
