package fdk123.myinfinitecreation.network;

import fdk123.myinfinitecreation.reliquified.LichCrown;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.api.CuriosApi;
import twilightforest.entity.projectile.TwilightWandBolt;

import java.util.function.Supplier;

public record LichTwilightBoltPacket() {
    public static void encode(LichTwilightBoltPacket packet, FriendlyByteBuf buffer) {
    }

    public static LichTwilightBoltPacket decode(FriendlyByteBuf buffer) {
        return new LichTwilightBoltPacket();
    }

    public static void handle(LichTwilightBoltPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.isSpectator()) return;
            CuriosApi.getCuriosInventory(player).ifPresent(curios -> {
                for (var slot : curios.findCurios(LichCrown.ITEM.get())) {
                    ItemStack stack = slot.stack();
                    if (!(stack.getItem() instanceof LichCrown crown) || !crown.hasGem(stack, "twilight")
                            || stack.getOrCreateTag().getInt("mic_lich_twilight_cooldown") > 0) continue;
                    Vec3 eye = player.getEyePosition(1);
                    Vec3 look = player.getViewVector(1);
                    double range = 64;
                    EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, eye.add(look.scale(range)),
                            player.getBoundingBox().expandTowards(look.scale(range)).inflate(1),
                            candidate -> candidate instanceof LivingEntity && candidate.isAlive()
                                    && candidate != player && !player.isAlliedTo(candidate)
                                    && player.hasLineOfSight(candidate), range * range);
                    if (hit == null) return;
                    TwilightWandBolt bolt = new TwilightWandBolt(player.level(), player);
                    bolt.setPos(eye);
                    bolt.setNoGravity(true);
                    bolt.setDeltaMovement(look.scale(crown.getAbilityValue(stack, "twilight", "speed_scale")));
                    bolt.getPersistentData().putBoolean("mic_lich_twilight_bolt", true);
                    bolt.getPersistentData().putFloat("mic_lich_twilight_damage",
                            (float) crown.getAbilityValue(stack, "twilight", "damage"));
                    player.level().addFreshEntity(bolt);
                    stack.getOrCreateTag().putInt("mic_lich_twilight_cooldown", 50);
                    player.level().playSound(null, player, SoundEvents.EVOKER_CAST_SPELL,
                            SoundSource.PLAYERS, 1, 1.2f);
                    break;
                }
            });
        });
        context.setPacketHandled(true);
    }
}
