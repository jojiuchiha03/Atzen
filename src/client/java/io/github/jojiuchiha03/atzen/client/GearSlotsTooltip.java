package io.github.jojiuchiha03.atzen.client;

import io.github.jojiuchiha03.atzen.item.AtzenRodItem;
import io.github.jojiuchiha03.atzen.item.GearSlot;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A rod's tooltip: six inventory-style slots (reel, spool, line, leader, hook, bait) with their contents, the rod's
 * attributes and what the whole set does.
 */
final class GearSlotsTooltip implements ClientTooltipComponent {
    private static final int SLOT = 18;
    private static final int ROW = 20;
    private final AtzenRodItem.GearTooltip data;

    GearSlotsTooltip(AtzenRodItem.GearTooltip data) { this.data = data; }

    private ItemStack stack(GearSlot slot) { return data.slots()[slot.ordinal()]; }

    private Component label(GearSlot slot) {
        ItemStack stack = stack(slot);
        Component content = stack.isEmpty() ? Component.translatable("tooltip.atzen.gear.empty")
                : slot == GearSlot.BAIT && stack.getCount() > 1 ? Component.translatable("tooltip.atzen.gear.count", stack.getHoverName(), stack.getCount())
                : stack.getHoverName();
        return Component.translatable(slot.translationKey(), content);
    }

    private static String number(double value) {
        return String.format(Locale.GERMAN, value >= 10 ? "%.0f" : "%.1f", value);
    }

    private Component[] summary() {
        var rig = data.rig();
        var rod = data.rod();
        Component modules = Component.empty();
        for (int i = 0; i < data.modules().size(); i++) {
            if (i > 0) modules = modules.copy().append(", ");
            modules = modules.copy().append(Component.translatable(data.modules().get(i).translationKey()));
        }
        Component extras = Component.translatable("tooltip.atzen.gear.modules",
                data.modules().isEmpty() ? Component.translatable("tooltip.atzen.gear.empty") : modules, data.upgrades());
        return new Component[] {
                Component.translatable("tooltip.atzen.gear.rod", number(rod.load()), number(rod.power()),
                        Math.round(rod.flex() * 100), Math.round(rod.sensitivity() * 100), number(rod.castMin()),
                        number(rod.castMax()), number(rod.fishMin()), number(rod.fishMax()), number(data.cast())),
                Component.translatable("tooltip.atzen.gear.stats", number(rig.lineStrength()), number(rig.capacity()),
                        number(rig.reelSpeed() * 20), number(rig.reelForce())),
                Component.translatable("tooltip.atzen.gear.ability", Component.translatable(rig.ability().translationKey())),
                extras};
    }

    @Override public int getHeight(Font font) { return GearSlot.values().length * ROW + summary().length * 10 + 2; }

    @Override
    public int getWidth(Font font) {
        int width = 0;
        for (Component line : summary()) width = Math.max(width, font.width(line));
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
        int[] colours = {0xFFB8C4CC, 0xFF7FD8C8, 0xFFE8C860, 0xFFC8A0F0};
        Component[] lines = summary();
        for (int i = 0; i < lines.length; i++) {
            graphics.text(font, lines[i], x, y + GearSlot.values().length * ROW + 2 + i * 10, colours[i], true);
        }
    }
}
