package io.github.jojiuchiha03.atzen.network;

import io.github.jojiuchiha03.atzen.Atzen;
import io.github.jojiuchiha03.atzen.battle.FishAction;
import io.github.jojiuchiha03.atzen.battle.FishingBattle;
import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import io.github.jojiuchiha03.atzen.fish.FishSpecimen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The state of a fight as the angler's client needs it for the gauge and the camera. Tension and drag are
 * shares of the line's breaking load; distances in metres.
 */
public record BattlePayload(int battleId, FishingBattle.Result result, FishAction action, FishAction pending,
                            float tension, float drag, float stamina, float distance, float lineOut, float capacity,
                            float hookHold, int sideDir, int flags, int secondsLeft, int quality, FishSpecies species,
                            float lengthCm, float weightKg, int bossPhase, int fishEntityId, int strike, int landTicks,
                            float netWindow)
        implements CustomPacketPayload {
    public static final int SLACK = 1, LANDABLE = 2, PRACTICE = 8, FAKING = 16, TWIST = 32, GIVING = 64;
    public static final Type<BattlePayload> TYPE = new Type<>(Atzen.id("battle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BattlePayload> CODEC = new StreamCodec<>() {
        public BattlePayload decode(RegistryFriendlyByteBuf buf) {
            int id = buf.readVarInt();
            var result = buf.readEnum(FishingBattle.Result.class);
            var action = buf.readEnum(FishAction.class);
            int pending = buf.readVarInt();
            return new BattlePayload(id, result, action, pending == 0 ? null : FishAction.values()[Math.clamp(pending - 1, 0, FishAction.values().length - 1)],
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readByte(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readEnum(FishSpecies.class), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readFloat());
        }

        public void encode(RegistryFriendlyByteBuf buf, BattlePayload value) {
            buf.writeVarInt(value.battleId());
            buf.writeEnum(value.result());
            buf.writeEnum(value.action());
            buf.writeVarInt(value.pending() == null ? 0 : value.pending().ordinal() + 1);
            buf.writeFloat(value.tension());
            buf.writeFloat(value.drag());
            buf.writeFloat(value.stamina());
            buf.writeFloat(value.distance());
            buf.writeFloat(value.lineOut());
            buf.writeFloat(value.capacity());
            buf.writeFloat(value.hookHold());
            buf.writeByte(value.sideDir());
            buf.writeVarInt(value.flags());
            buf.writeVarInt(value.secondsLeft());
            buf.writeVarInt(value.quality());
            buf.writeEnum(value.species());
            buf.writeFloat(value.lengthCm());
            buf.writeFloat(value.weightKg());
            buf.writeVarInt(value.bossPhase());
            buf.writeVarInt(value.fishEntityId());
            buf.writeVarInt(value.strike());
            buf.writeVarInt(value.landTicks());
            buf.writeFloat(value.netWindow());
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public boolean has(int flag) { return (flags & flag) != 0; }

    public static BattlePayload of(int id, FishingBattle battle, boolean practice, FishSpecimen specimen, int fishEntityId,
                                   int strike, float netWindow) {
        double strength = battle.rig().lineStrength();
        int flags = (battle.slack() ? SLACK : 0) | (battle.landable() ? LANDABLE : 0) | (practice ? PRACTICE : 0)
                | (battle.faking() ? FAKING : 0) | (battle.twisting() ? TWIST : 0) | (battle.giving() ? GIVING : 0);
        return new BattlePayload(id, battle.result(), battle.action(), battle.pending(),
                (float) (battle.tension() / strength), (float) (battle.dragKg() / strength), (float) battle.stamina(),
                (float) battle.distance(), (float) battle.distance(), (float) battle.rig().capacity(), (float) battle.hookHold(),
                battle.sideDir(), flags, Math.max(0, (battle.maxTicks() - battle.elapsed() + 19) / 20), battle.quality(),
                specimen.species(), (float) specimen.lengthCm(), (float) specimen.weightKg(), battle.bossPhase(),
                fishEntityId, strike, battle.landableTicks(), netWindow);
    }
}
