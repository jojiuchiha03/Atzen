package io.github.jojiuchiha03.atzen.network;

import io.github.jojiuchiha03.atzen.Atzen;
import io.github.jojiuchiha03.atzen.battle.FishStyle;
import io.github.jojiuchiha03.atzen.battle.FishingBattle;
import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import io.github.jojiuchiha03.atzen.fish.FishSpecimen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BattlePayload(int battleId, FishStyle style, FishingBattle.Phase phase,
                            FishingBattle.Result result, float tension, float progress,
                            int secondsLeft, int quality, boolean slack, boolean practice,
                            FishSpecies species, float lengthCm, float weightKg)
        implements CustomPacketPayload {
    public static final Type<BattlePayload> TYPE = new Type<>(Atzen.id("battle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BattlePayload> CODEC = new StreamCodec<>() {
        public BattlePayload decode(RegistryFriendlyByteBuf buf) {
            return new BattlePayload(buf.readVarInt(), buf.readEnum(FishStyle.class),
                    buf.readEnum(FishingBattle.Phase.class), buf.readEnum(FishingBattle.Result.class),
                    buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarInt(),
                    buf.readBoolean(), buf.readBoolean(), buf.readEnum(FishSpecies.class), buf.readFloat(), buf.readFloat());
        }
        public void encode(RegistryFriendlyByteBuf buf, BattlePayload value) {
            buf.writeVarInt(value.battleId());
            buf.writeEnum(value.style());
            buf.writeEnum(value.phase());
            buf.writeEnum(value.result());
            buf.writeFloat(value.tension());
            buf.writeFloat(value.progress());
            buf.writeVarInt(value.secondsLeft());
            buf.writeVarInt(value.quality());
            buf.writeBoolean(value.slack());
            buf.writeBoolean(value.practice());
            buf.writeEnum(value.species());
            buf.writeFloat(value.lengthCm());
            buf.writeFloat(value.weightKg());
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static BattlePayload of(int id, FishingBattle battle, boolean practice, FishSpecimen specimen) {
        return new BattlePayload(id, battle.style(), battle.phase(), battle.result(),
                (float) battle.tension(), (float) battle.progress(),
                Math.max(0, (FishingBattle.MAX_TICKS - battle.elapsed() + 19) / 20),
                battle.quality(), battle.slackTicks() > 0, practice,
                specimen == null ? FishSpecies.PERCH : specimen.species(),
                specimen == null ? 0 : (float) specimen.lengthCm(), specimen == null ? 0 : (float) specimen.weightKg());
    }
}
