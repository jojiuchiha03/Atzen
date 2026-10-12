package io.github.jojiuchiha03.atzen.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Rod holders (Anforderung 13): a single holder, a double stand and a rod pod with an electronic alarm. Click with a
 * rod to cast it out in front of the holder; when the indicator goes, click the holder to take the rod and strike.
 * An unattended bite costs the bait, sometimes the lure, and a big fish can pull a rod from a simple holder.
 */
public final class RodHolderBlock extends BaseEntityBlock {
    public enum Kind {
        SINGLE(1, false), DOUBLE(2, false), POD(3, true);

        private final int rods;
        private final boolean alarm;

        Kind(int rods, boolean alarm) {
            this.rods = rods;
            this.alarm = alarm;
        }

        public int rods() { return rods; }
        public boolean alarm() { return alarm; }
    }

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty BITE = BooleanProperty.create("bite");
    public static final BooleanProperty LOADED = BooleanProperty.create("loaded");
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 12, 13);
    private final Kind kind;

    public RodHolderBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BITE, false).setValue(LOADED, false));
    }

    public Kind kind() { return kind; }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new RodHolderBlock(properties, kind));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, BITE, LOADED); }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RodHolderBlockEntity(pos, state); }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, AtzenBlocks.ROD_HOLDER_ENTITY, RodHolderBlockEntity::tick);
    }

    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                                    InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RodHolderBlockEntity holder) || !RodHolderBlockEntity.isRod(stack)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (player instanceof ServerPlayer serverPlayer) holder.place(serverPlayer, hand);
        return InteractionResult.SUCCESS;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RodHolderBlockEntity holder)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) holder.take(serverPlayer);
        return InteractionResult.SUCCESS;
    }
}
