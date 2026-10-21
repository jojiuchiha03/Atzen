package io.github.jojiuchiha03.atzen.client;

import io.github.jojiuchiha03.atzen.item.AtzenRodItem;
import io.github.jojiuchiha03.atzen.item.GearSlot;
import io.github.jojiuchiha03.atzen.item.RodMods;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A rod's tooltip: three slots side by side (reel, line, bait) with a label under each, then two short lines on what
 * the rod holds and how fast it reels, and the workshop upgrades as stars.
 */
final class GearSlotsTooltip implements ClientTooltipComponent {
    private static final int SLOT = 20;
    private static final int COLUMN = 54;
    private final AtzenRodItem.GearTooltip data;

    GearSlotsTooltip(AtzenRodItem.GearTooltip data) { this.data = data; }

    private ItemStack stack(GearSlot slot) { return data.slots()[slot.ordinal()]; }

    private static String number(double value) {
        return String.format(Locale.GERMAN, value >= 10 ? "%.0f" : "%.1f", value);
    }

    private Component[] lines() {
        var rig = data.rig();
        String stars = "★".repeat(data.upgrades()) + "☆".repeat(RodMods.MAX_UPGRADES - data.upgrades());
        return new Component[] {
                Component.translatable("tooltip.atzen.gear.holds", number(Math.min(rig.lineStrength(), rig.rodLoad()))),
                Component.translatable("tooltip.atzen.gear.speed", number(rig.reelSpeed() * 20), number(rig.reelForce()),
                        number(rig.capacity())),
                Component.translatable("tooltip.atzen.gear.upgrades", stars)};
    }

    @Override public int getHeight(Font font) { return SLOT + 12 + lines().length * 10 + 4; }

    @Override
    public int getWidth(Font font) {
        int width = GearSlot.values().length * COLUMN;
        for (Component line : lines()) width = Math.max(width, font.width(line));
        return width;
    }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
        for (GearSlot slot : GearSlot.values()) {
            int left = x + slot.ordinal() * COLUMN + (COLUMN - SLOT) / 2;
            int top = y + 2;
            ItemStack stack = stack(slot);
            graphics.fill(left, top, left + SLOT, top + SLOT, stack.isEmpty() ? 0xFF2A2F36 : 0xFF3A4652);
            graphics.outline(left, top, SLOT, SLOT, stack.isEmpty() ? 0xFF4A525C : 0xFF7FD8C8);
            if (!stack.isEmpty()) {
                graphics.item(stack, left + 2, top + 2);
                graphics.itemDecorations(font, stack, left + 2, top + 2);
            }
        }
    }

    @Override
    public void extractText(GuiGraphicsExtractor graphics, Font font, int x, int y) {
        for (GearSlot slot : GearSlot.values()) {
            Component label = Component.translatable(slot.translationKey());
            int centre = x + slot.ordinal() * COLUMN + COLUMN / 2;
            graphics.text(font, label, centre - font.width(label) / 2, y + SLOT + 4, stack(slot).isEmpty() ? 0xFF7A828C : 0xFFD8E0E6, false);
        }
        int[] colours = {0xFF7FD8C8, 0xFFB8C4CC, 0xFFFFC94A};
        Component[] lines = lines();
        for (int i = 0; i < lines.length; i++) graphics.text(font, lines[i], x, y + SLOT + 16 + i * 10, colours[i], true);
    }
}
