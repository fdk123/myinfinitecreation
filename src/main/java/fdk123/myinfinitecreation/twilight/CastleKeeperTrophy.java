package fdk123.myinfinitecreation.twilight;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

public final class CastleKeeperTrophy {
    private static final Rarity TROPHY_RARITY = Rarity.create("MIC_TWILIGHT_TROPHY", ChatFormatting.DARK_GREEN);
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MyInfiniteCreation.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);

    public static final RegistryObject<Block> BLOCK = BLOCKS.register("castle_keeper_trophy",
            () -> new TrophyBlock(Block.Properties.of().strength(1.0F).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<Item> ITEM = ITEMS.register("castle_keeper_trophy",
            () -> new CastleKeeperTrophyItem(BLOCK.get(), new Item.Properties().rarity(TROPHY_RARITY)));

    private CastleKeeperTrophy() {}

    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            event.accept(ITEM.get());
        }
    }

    private static final class TrophyBlock extends HorizontalDirectionalBlock {
        private static final VoxelShape NORTH_SOUTH_SHAPE = Block.box(0.5, 2, 5, 15.5, 16, 11);
        private static final VoxelShape EAST_WEST_SHAPE = Block.box(5, 2, 0.5, 11, 16, 15.5);

        private TrophyBlock(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(FACING).getAxis() == Direction.Axis.X ? EAST_WEST_SHAPE : NORTH_SOUTH_SHAPE;
        }

        @Override
        public BlockState rotate(BlockState state, Rotation rotation) {
            return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        }

        @Override
        public BlockState mirror(BlockState state, Mirror mirror) {
            return state.rotate(mirror.getRotation(state.getValue(FACING)));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }
    }
}
