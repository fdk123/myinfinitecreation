package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class HydraFireEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MyInfiniteCreation.MOD_ID);

    public static final RegistryObject<EntityType<HydraFireProjectile>> PROJECTILE = ENTITIES.register("hydra_fire",
            () -> EntityType.Builder.<HydraFireProjectile>of(HydraFireProjectile::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f).clientTrackingRange(8).updateInterval(1)
                    .build(MyInfiniteCreation.MOD_ID + ":hydra_fire"));

    public static final RegistryObject<EntityType<HydraFirePuddle>> PUDDLE = ENTITIES.register("hydra_fire_puddle",
            () -> EntityType.Builder.<HydraFirePuddle>of(HydraFirePuddle::new, MobCategory.MISC)
                    .sized(4.0f, 0.3f).clientTrackingRange(8).updateInterval(1)
                    .build(MyInfiniteCreation.MOD_ID + ":hydra_fire_puddle"));

    private HydraFireEntities() {
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
