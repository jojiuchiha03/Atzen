package io.github.jojiuchiha03.atzen.client;

import net.fabricmc.api.ClientModInitializer;

/** Nur auf dem Client: Renderer, Tastenbelegungen, HUD. Server-Code darf hiervon nichts kennen. */
public final class AtzenClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
    }
}
