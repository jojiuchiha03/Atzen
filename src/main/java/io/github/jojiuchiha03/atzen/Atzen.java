package io.github.jojiuchiha03.atzen;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Einstieg der Mod (Server und Client). Hier wird alles registriert. */
public final class Atzen implements ModInitializer {
    public static final String MOD_ID = "atzen";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModItems.register();
        LOG.info("Atzen geladen");
    }
}
