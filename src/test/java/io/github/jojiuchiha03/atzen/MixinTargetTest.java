package io.github.jojiuchiha03.atzen;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class MixinTargetTest {
    @Test void fishingHooksAndLootInvocationMatchMinecraft262Bytecode() throws IOException {
        // Read bytecode only: do not initialize Minecraft or launch a world.
        try (var input = getClass().getClassLoader().getResourceAsStream("net/minecraft/world/entity/projectile/FishingHook.class")) {
            assertNotNull(input);
            var node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            assertTrue(node.fields.stream().anyMatch(f -> f.name.equals("nibble") && f.desc.equals("I")));
            assertTrue(node.methods.stream().anyMatch(m -> m.name.equals("catchingFish") && m.desc.equals("(Lnet/minecraft/core/BlockPos;)V")));
            assertTrue(node.methods.stream().anyMatch(m -> m.name.equals("shouldStopFishing") && m.desc.equals("(Lnet/minecraft/world/entity/player/Player;)Z")));
            var retrieve = node.methods.stream().filter(m -> m.name.equals("retrieve") && m.desc.equals("(Lnet/minecraft/world/item/ItemStack;)I")).findFirst().orElseThrow();
            int lootCalls = 0;
            for (var instruction : retrieve.instructions) if (instruction instanceof MethodInsnNode call
                    && call.owner.equals("net/minecraft/world/level/storage/loot/LootTable") && call.name.equals("getRandomItems")
                    && call.desc.equals("(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;")) lootCalls++;
            assertEquals(1, lootCalls, "The catch replacement must target exactly one vanilla loot call");
        }
    }
}
