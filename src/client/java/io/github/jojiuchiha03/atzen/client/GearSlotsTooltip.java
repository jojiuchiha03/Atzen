package io.github.jojiuchiha03.atzen.client;

import io.github.jojiuchiha03.atzen.item.AtzenRodItem;
import io.github.jojiuchiha03.atzen.item.GearSlot;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** A rod's tooltip: three inventory-style slots (reel, line, bait) with their contents and what the set does. */
final class GearSlotsTooltip implements ClientTooltipComponent {
    private static final int SLOT = 18;
    private static final int ROW = 20;
    private final AtzenRodItem.GearTooltip data;

    GearSlotsTooltip(AtzenRodItem.GearTooltip data) { this.data = data; }

    private ItemStack stack(GearSlot slot) {
        return switch (slot) {
            case REEL -> data.reel();
            case LINE -> data.line();
            case BAIT -> data.bait();
        };
    }

    private Component label(GearSlot slot) {
        ItemStack stack = stack(slot);
        Component content = stack.isEmpty() ? Component.translatable("tooltip.atzen.gear.empty")
                : slot == GearSlot.BAIT ? Component.translatable("tooltip.atzen.gear.count", stack.getHoverName(), stack.getCount())
                : stack.getHoverName();
        return Component.translatable(slot.translationKey(), content);
    }

    private static String percent(double factor) {
        long change = Math.round((factor - 1) * 100);
        return String.format(Locale.ROOT, "%s%d %%", change > 0 ? "+" : change < 0 ? "−" : "±", Math.abs(change));
    }

    private Component summary() {
        double[] e = data.effect();
        return Component.translatable("tooltip.atzen.gear.stats", percent(e[0]), percent(e[1]), percent(e[2]));
    }

    @Override public int getHeight(Font font) { return GearSlot.values().length * ROW + 12; }

    @Override
    public int getWidth(Font font) {
        int width = font.width(summary());
        for (GearSlot slot : GearSlot.values()) width = Math.max(width, SLOT + 6 + font.width(label(slot)));
        return width;
    }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
        for (GearSlot slot : GearSlot.values()) {
            int top = y + slot.ordinal() * ROW + 1;
            // Vanilla slot look: dark top-left edge, light bottom-right edge, grey inside.
            graphics.fill(x, top, x + SLOT, top + SLOT, 0xFF8B8B8B);
            graphics.fill(x, top, x + SLOT - 1, top + 1, 0xFF373737);
            graphics.fill(x, top, x + 1, top + SLOT - 1, 0xFF373737);
            graphics.fill(x + 1, top + SLOT - 1, x + SLOT, top + SLOT, 0xFFFFFFFF);
            graphics.fill(x + SLOT - 1, top + 1, x + SLOT, top + SLOT, 0xFFFFFFFF);
            ItemStack stack = stack(slot);
            if (!stack.isEmpty()) {
                graphics.item(stack, x + 1, top + 1);
                graphics.itemDecorations(font, stack, x + 1, top + 1);
            }
        }
    }

    @Override
    public void extractText(GuiGraphicsExtractor graphics, Font font, int x, int y) {
        for (GearSlot slot : GearSlot.values()) {
            boolean empty = stack(slot).isEmpty();
            graphics.text(font, label(slot), x + SLOT + 5, y + slot.ordinal() * ROW + 6, empty ? 0xFF9A9A9A : 0xFFFFFFFF, true);
        }
        graphics.text(font, summary(), x, y + GearSlot.values().length * ROW + 2, 0xFF7FD8C8, true);
    }
}
