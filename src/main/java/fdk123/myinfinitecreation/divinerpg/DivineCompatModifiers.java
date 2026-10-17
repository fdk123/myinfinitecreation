package fdk123.myinfinitecreation.divinerpg;

import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

/**
 * Static Tinkers modifiers whose behavior cannot be expressed by a datapack module.
 * The registration API is Tinkers 3.11's modifier manager, not the old Forge registry.
 */
public final class DivineCompatModifiers {
    public static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create("myinfinitecreation");

    public static final StaticModifier<TwilightModifier> TWILIGHT = MODIFIERS.register("twilight", TwilightModifier::new);
    public static final StaticModifier<DivineInterventionModifier> DIVINE_INTERVENTION = MODIFIERS.register("divine_intervention", DivineInterventionModifier::new);
    public static final StaticModifier<MagicalModifier> MAGICAL = MODIFIERS.register("magical", MagicalModifier::new);
    public static final StaticModifier<CorruptingModifier> CORRUPTING = MODIFIERS.register("corrupting", CorruptingModifier::new);

    private DivineCompatModifiers() {}

    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        MODIFIERS.register(bus);
    }

    /** Twilight: stronger attack damage in DivineRPG dimensions, weaker in vanilla dimensions. */
    public static final class TwilightModifier extends Modifier implements MeleeDamageModifierHook {
        @Override
        protected void registerHooks(Builder builder) {
            builder.addHook(this, ModifierHooks.MELEE_DAMAGE);
        }

        @Override
        public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context,
                                    float baseDamage, float damage) {
            String dimension = context.getAttacker().level().dimension().location().toString();
            float factor = switch (dimension) {
                case "divinerpg:eden" -> 2.0f + modifier.getLevel() * 0.02f;
                case "divinerpg:apalachia" -> 2.0f + modifier.getLevel() * 0.04f;
                case "divinerpg:wildwood" -> 2.0f + modifier.getLevel() * 0.06f;
                case "divinerpg:skythern" -> 2.0f + modifier.getLevel() * 0.08f;
                case "divinerpg:mortum" -> 2.0f + modifier.getLevel() * 0.10f;
                case "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end" -> 0.5f;
                default -> 1.0f;
            };
            return damage * factor;
        }
    }

    /** Corrupting: trade the wielder's health for short combat and mining buffs. */
    public static final class CorruptingModifier extends Modifier implements MeleeHitModifierHook {
        @Override
        protected void registerHooks(Builder builder) {
            builder.addHook(this, ModifierHooks.MELEE_HIT);
        }

        @Override
        public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier,
                                  ToolAttackContext context, float damageDealt) {
            LivingEntity attacker = context.getAttacker();
            if (attacker.level().isClientSide || !attacker.isAlive()) {
                return;
            }
            int level = modifier.getLevel();
            float healthFloor = 20.0f / (level + 1.0f);
            if (attacker.getHealth() > healthFloor) {
                attacker.setHealth(healthFloor);
            }
            attacker.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.HUNGER, 8, level));
            attacker.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, level * 8, level / 2));
            attacker.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DIG_SPEED, level * 8, level / 2));
        }
    }

    /** Marker modifier; the actual death interception is handled on the Forge death event. */
    public static final class DivineInterventionModifier extends Modifier {
    }

    /** Arcana-powered bonus magic damage. Arcana is accessed reflectively so the addon remains loadable without DivineRPG. */
    public static final class MagicalModifier extends Modifier implements MeleeHitModifierHook {
        @Override
        protected void registerHooks(Builder builder) {
            builder.addHook(this, ModifierHooks.MELEE_HIT);
        }

        @Override
        public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier,
                                  ToolAttackContext context, float damageDealt) {
            if (context.getLivingTarget() == null || context.getLivingTarget().level().isClientSide) {
                return;
            }
            LivingEntity attacker = context.getAttacker();
            int cost = modifier.getLevel() * 15;
            if (!ArcanaAccess.consume(attacker, cost)) {
                return;
            }
            float attackDamage = tool.getStats().get(ToolStats.ATTACK_DAMAGE);
            float bonus = attackDamage * (modifier.getLevel() / 5.0f);
            if (bonus > 0) {
                context.getLivingTarget().hurt(attacker.damageSources().magic(), bonus);
            }
        }
    }

    private static final class ArcanaAccess {
        private ArcanaAccess() {}

        static boolean consume(LivingEntity entity, int cost) {
            try {
                Class<?> providerClass = Class.forName("divinerpg.capability.ArcanaProvider");
                Object capability = providerClass.getField("ARCANA").get(null);
                Object optional = entity.getClass().getMethod("getCapability", net.minecraftforge.common.capabilities.Capability.class)
                        .invoke(entity, capability);
                if (!(boolean) optional.getClass().getMethod("isPresent").invoke(optional)) {
                    return false;
                }
                Object arcana = optional.getClass().getMethod("orElse", Object.class).invoke(optional, new Object[]{null});
                if (arcana == null) {
                    return false;
                }
                float amount = (float) arcana.getClass().getMethod("getAmount", boolean.class).invoke(arcana, false);
                if (amount < cost) {
                    return false;
                }
                arcana.getClass().getMethod("modifyAmount", LivingEntity.class, float.class)
                        .invoke(arcana, entity, -((float) cost));
                return true;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return false;
            }
        }
    }
}
