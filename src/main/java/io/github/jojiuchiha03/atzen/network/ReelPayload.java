package io.github.jojiuchiha03.atzen.network;

import io.github.jojiuchiha03.atzen.Atzen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ReelPayload(int battleId, boolean reeling, boolean cancel) implements CustomPacketPayload {
    public static final Type<ReelPayload> TYPE = new Type<>(Atzen.id("reel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReelPayload> CODEC = new StreamCodec<>() {
        public ReelPayload decode(RegistryFriendlyByteBuf buf) {
            return new ReelPayload(buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
        }
        public void encode(RegistryFriendlyByteBuf buf, ReelPayload value) {
            buf.writeVarInt(value.battleId());
            buf.writeBoolean(value.reeling());
            buf.writeBoolean(value.cancel());
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
