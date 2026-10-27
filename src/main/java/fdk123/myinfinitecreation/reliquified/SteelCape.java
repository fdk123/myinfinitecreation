package fdk123.myinfinitecreation.reliquified;

import fdk123.myinfinitecreation.MyInfiniteCreation;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;
import twilightforest.entity.ChainBlock;
import twilightforest.init.TFEntities;

public final class SteelCape extends RelicItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MyInfiniteCreation.MOD_ID);
    public static final RegistryObject<Item> ITEM = ITEMS.register("steel_cape", SteelCape::new);

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("iron_guard")
                                .stat(StatData.builder("flat_armor")
                                        .initialValue(0.2, 0.4).upgradeModifier(UpgradeOperation.ADD, 0.16).build())
                                .stat(StatData.builder("chance")
                                        .initialValue(0.1, 0.25).upgradeModifier(UpgradeOperation.ADD, 0.025).build())
                                .stat(StatData.builder("damage")
                                        .initialValue(4, 6).upgradeModifier(UpgradeOperation.ADD, 0.6).build())
                                .stat(StatData.builder("stun_duration")
                                        .initialValue(10, 20).upgradeModifier(UpgradeOperation.ADD, 8).build())
                                .build())
                        .build())
                .leveling(LevelingData.builder().build())
                .build();
    }

    public float chainDamage(ItemStack stack) {
        return (float) getAbilityValue(stack, "iron_guard", "damage");
    }

    public int stunDuration(ItemStack stack) {
        return Math.max(1, (int) Math.round(getAbilityValue(stack, "iron_guard", "stun_duration")));
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity wearer = event.getEntity();
        if (wearer.level().isClientSide() || event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) {
            return;
        }
        CuriosApi.getCuriosInventory(wearer).ifPresent(curios -> {
            for (var slot : curios.findCurios(ITEM.get())) {
                ItemStack stack = slot.stack();
                if (!(stack.getItem() instanceof SteelCape cape)) {
                    continue;
                }
                float remaining = event.getAmount() - (float) cape.getAbilityValue(stack, "iron_guard", "flat_armor");
                event.setAmount(Math.max(0.001f, remaining));
                if (remaining <= 0 || !(event.getSource().getEntity() instanceof LivingEntity attacker)
                        || wearer.getRandom().nextDouble() >= cape.getAbilityValue(stack, "iron_guard", "chance")) {
                    continue;
                }
                ChainBlock chain = new ChainBlock(TFEntities.CHAIN_BLOCK.get(), wearer.level(), wearer,
                        InteractionHand.MAIN_HAND, stack);
                chain.setPos(wearer.getX(), wearer.getY() + wearer.getBbHeight() / 2, wearer.getZ());
                Vec3 target = attacker.position().add(0, attacker.getBbHeight() / 2, 0);
                Vec3 direction = target.subtract(chain.position()).normalize();
                chain.shoot(direction.x, direction.y, direction.z, 1.5f, 1f);
                if (wearer.level().addFreshEntity(chain)) {
                    cape.addExperience(wearer, stack, 1);
                }
            }
        });
    }
}
