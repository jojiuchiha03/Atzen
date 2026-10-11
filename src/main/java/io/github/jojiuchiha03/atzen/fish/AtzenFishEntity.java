package io.github.jojiuchiha03.atzen.fish;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.Random;
import java.util.stream.Stream;

public final class AtzenFishEntity extends Cod {
    private final FishSpecies species;
    private FishSpecimen specimen;

    public AtzenFishEntity(EntityType<? extends Cod> type, Level level, FishSpecies species) {
        super(type, level);
        this.species = species;
        setSpecimen(species.roll(new Random(getRandom().nextLong())));
    }

    public FishSpecimen specimen() { return specimen; }

    private void setSpecimen(FishSpecimen specimen) {
        this.specimen = specimen;
        if (!level().isClientSide()) {
            // Scale is a vanilla synchronized attribute: visuals and hitbox follow the same size.
            getAttribute(Attributes.SCALE).setBaseValue(specimen.lengthCm() / species.modelLengthCm());
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(3 + specimen.sizeFraction() * 5);
            setHealth(getMaxHealth());
        }
    }

    @Override public int getMaxSchoolSize() {
        return species == FishSpecies.PIKE || species == FishSpecies.CATFISH ? 1 : 5;
    }

    @Override public AbstractSchoolingFish startFollowing(AbstractSchoolingFish leader) {
        return getMaxSchoolSize() > 1 && leader.getType() == getType() ? super.startFollowing(leader) : this;
    }

    @Override public void addFollowers(Stream<? extends AbstractSchoolingFish> fish) {
        super.addFollowers(fish.filter(other -> other.getType() == getType()));
    }

    @Override public ItemStack getBucketItemStack() { return new ItemStack(AtzenFishRegistry.bucket(species)); }

    @Override public void saveToBucketTag(ItemStack bucket) {
        super.saveToBucketTag(bucket);
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> {
            tag.putDouble("AtzenLength", specimen.lengthCm());
            tag.putDouble("AtzenWeight", specimen.weightKg());
        });
        bucket.set(DataComponents.LORE, new ItemLore(FishCatch.measurementLore(specimen)));
    }

    @Override public void loadFromBucketTag(CompoundTag tag) {
        restore(tag.getDoubleOr("AtzenLength", specimen.lengthCm()), tag.getDoubleOr("AtzenWeight", specimen.weightKg()));
        // Restore maximum health before vanilla restores the saved current health.
        super.loadFromBucketTag(tag);
    }

    @Override protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putDouble("AtzenLength", specimen.lengthCm());
        output.putDouble("AtzenWeight", specimen.weightKg());
    }

    @Override protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        restore(input.getDoubleOr("AtzenLength", specimen.lengthCm()), input.getDoubleOr("AtzenWeight", specimen.weightKg()));
    }

    private void restore(double length, double weight) {
        if (Double.isFinite(length) && Double.isFinite(weight) && weight > 0) {
            float health = getHealth();
            setSpecimen(new FishSpecimen(species, Math.clamp(length, species.minLength(), species.maxLength()), weight));
            setHealth(Math.min(health, getMaxHealth()));
        }
    }

    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(level, source, causedByPlayer);
        spawnAtLocation(level, FishCatch.create(specimen, level, blockPosition(), -1, ""));
    }
}
