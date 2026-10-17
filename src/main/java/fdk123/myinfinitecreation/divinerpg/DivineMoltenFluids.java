package fdk123.myinfinitecreation.divinerpg;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Forge 1.20.1 versions of the DivineRPG Extras molten liquids. */
public final class DivineMoltenFluids {
    public static final DeferredRegister<FluidType> TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, MyInfiniteCreation.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, MyInfiniteCreation.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MyInfiniteCreation.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);

    private static final ResourceLocation STILL = new ResourceLocation(MyInfiniteCreation.MOD_ID, "block/molten_still");
    private static final ResourceLocation FLOWING = new ResourceLocation(MyInfiniteCreation.MOD_ID, "block/molten_flowing");
    private static final Map<String, Molten> ENTRIES = new LinkedHashMap<>();

    private DivineMoltenFluids() {}

    private static final class Molten {
        RegistryObject<FluidType> type;
        RegistryObject<ForgeFlowingFluid.Source> source;
        RegistryObject<ForgeFlowingFluid.Flowing> flowing;
        RegistryObject<LiquidBlock> block;
        RegistryObject<Item> bucket;

        ForgeFlowingFluid.Properties properties() {
            return new ForgeFlowingFluid.Properties(type, source, flowing).block(block).bucket(bucket);
        }
    }

    private static void add(String name, int color) {
        Molten molten = new Molten();
        molten.type = TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                .descriptionId("fluid." + MyInfiniteCreation.MOD_ID + "." + name)
                .density(3000).viscosity(6000).temperature(1000).lightLevel(15)
                .canSwim(false).canHydrate(false)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    @Override public ResourceLocation getStillTexture() { return STILL; }
                    @Override public ResourceLocation getFlowingTexture() { return FLOWING; }
                    @Override public int getTintColor() { return color; }
                });
            }
        });
        molten.source = FLUIDS.register(name, () -> new ForgeFlowingFluid.Source(molten.properties()));
        molten.flowing = FLUIDS.register("flowing_" + name, () -> new ForgeFlowingFluid.Flowing(molten.properties()));
        molten.block = BLOCKS.register(name, () -> new LiquidBlock(molten.source,
                BlockBehaviour.Properties.of().noCollission().strength(100.0F).noLootTable()
                        .replaceable().lightLevel(state -> 15).sound(SoundType.EMPTY)));
        molten.bucket = ITEMS.register(name + "_bucket", () -> new BucketItem(molten.source,
                new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
        ENTRIES.put(name, molten);
    }

    static {
        add("molten_aquatic", 0xFF1965E0);
        add("molten_arlemite", 0xFF2BBF26);
        add("molten_rupee", 0xFF1177D1);
        add("molten_realmite", 0xFFC9871C);
        add("molten_shadow_stone", 0xFF242322);
        add("molten_jungle_stone", 0xFF57D13F);
        add("molten_terran_stone", 0xFF369E21);
        add("molten_corrupted_stone", 0xFF3f19A8);
        add("molten_divine_stone", 0xFFC9C71A);
        add("molten_gray_diamond", 0xFF5C5C56);
        add("molten_blue_diamond", 0xFF135D8F);
        add("molten_red_diamond", 0xFFDB0F0F);
        add("molten_green_diamond", 0xFF4EFF45);
        add("molten_yellow_diamond", 0xFFF5F12F);
        add("molten_bloodgem", 0xFFD60F65);
        add("molten_molten_stone", 0xFFE8371C);
        add("molten_torridite", 0xFFC91010);
        add("molten_hellstone", 0xFF591E0A);
        add("molten_ender_stone", 0xFFA505AB);
        add("molten_bedrock_chunk", 0xFF19171A);
        add("molten_eden_chunk", 0xFFD13C17);
        add("molten_wildwood_chunk", 0xFF1768D1);
        add("molten_apalachia_chunk", 0xFFA317D1);
        add("molten_skythern_chunk", 0xFFA09DA1);
        add("molten_mortum_chunk", 0xFF1C1C1C);
        add("molten_ice_stone", 0xFF9BCFCD);
        add("molten_arcanium", 0xFF35C4BF);
        add("molten_teaker", 0xFF1A0000);
        add("molten_amthirmis", 0xFF260000);
        add("molten_darven", 0xFF420000);
        add("molten_cermile", 0xFF590000);
        add("molten_karos", 0xFF700101);
        add("molten_pardimal", 0xFF870000);
        add("molten_quadrotic", 0xFFA10000);
        add("molten_heliosis", 0xFFA30D0D);
        add("molten_arksiane", 0xFFD41111);
        add("molten_green_everstone", 0xFF06520D);
        add("molten_white_everstone", 0xFFF5F5F5);
        add("molten_black_everstone", 0xFF141414);
        add("molten_liquid_everstone", 0xFFC4C233);
        add("molten_fire_crystal", 0xFFC22727);
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        FLUIDS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}
