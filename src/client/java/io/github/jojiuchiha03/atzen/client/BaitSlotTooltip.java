package io.github.jojiuchiha03.atzen.client;

import io.github.jojiuchiha03.atzen.item.AtzenRodItem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;

/** Draws the rod's bait slot in its tooltip: an inventory-style slot with the bait, its name and count. */
final class BaitSlotTooltip implements ClientTooltipComponent {
    private static final int SLOT = 18;
    private final AtzenRodItem.BaitTooltip data;

    BaitSlotTooltip(AtzenRodItem.BaitTooltip data) { this.data = data; }

    private Component label() {
        return data.bait().isEmpty() ? Component.translatable("tooltip.atzen.bait_slot_empty")
                : Component.translatable("tooltip.atzen.bait_slot", data.bait().getHoverName(), data.bait().getCount());
    }

    @Override public int getHeight(Font font) { return SLOT + 4; }
    @Override public int getWidth(Font font) { return SLOT + 6 + font.width(label()); }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
        int top = y + 1;
        // Vanilla slot look: dark top-left edge, light bottom-right edge, grey inside.
        graphics.fill(x, top, x + SLOT, top + SLOT, 0xFF8B8B8B);
        graphics.fill(x, top, x + SLOT - 1, top + 1, 0xFF373737);
        graphics.fill(x, top, x + 1, top + SLOT - 1, 0xFF373737);
        graphics.fill(x + 1, top + SLOT - 1, x + SLOT, top + SLOT, 0xFFFFFFFF);
        graphics.fill(x + SLOT - 1, top + 1, x + SLOT, top + SLOT, 0xFFFFFFFF);
        if (!data.bait().isEmpty()) {
            graphics.item(data.bait(), x + 1, top + 1);
            graphics.itemDecorations(font, data.bait(), x + 1, top + 1);
        }
    }

    @Override
    public void extractText(GuiGraphicsExtractor graphics, Font font, int x, int y) {
        graphics.text(font, label(), x + SLOT + 5, y + 6, data.bait().isEmpty() ? 0xFF9A9A9A : 0xFFFFFFFF, true);
    }
}
