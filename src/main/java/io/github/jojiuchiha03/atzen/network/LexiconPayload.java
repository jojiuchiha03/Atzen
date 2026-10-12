package io.github.jojiuchiha03.atzen.network;

import io.github.jojiuchiha03.atzen.Atzen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Everything the fish lexicon shows: the angler's progress, today's orders, records, ranking and tournament. */
public record LexiconPayload(CompoundTag data) implements CustomPacketPayload {
    public static final Type<LexiconPayload> TYPE = new Type<>(Atzen.id("lexicon"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LexiconPayload> CODEC =
            ByteBufCodecs.COMPOUND_TAG.map(LexiconPayload::new, LexiconPayload::data).cast();

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
