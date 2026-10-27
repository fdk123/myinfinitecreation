package fdk123.myinfinitecreation.network;

import fdk123.myinfinitecreation.reliquified.ScaledCloak;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Supplier;

public record ScaledCloakClimbPacket(boolean touchingWall) {
    public static void encode(ScaledCloakClimbPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.touchingWall());
    }

    public static ScaledCloakClimbPacket decode(FriendlyByteBuf buffer) {
        return new ScaledCloakClimbPacket(buffer.readBoolean());
    }

    public static void handle(ScaledCloakClimbPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !packet.touchingWall() || !player.horizontalCollision) {
                return;
            }
            long tick = player.level().getGameTime();
            if (player.getPersistentData().getLong("mic_scaled_cloak_last_climb_tick") == tick) {
                return;
            }
            player.getPersistentData().putLong("mic_scaled_cloak_last_climb_tick", tick);
            CuriosApi.getCuriosInventory(player).ifPresent(curios -> {
                for (var slot : curios.findCurios(ScaledCloak.ITEM.get())) {
                    ItemStack stack = slot.stack();
                    if (stack.getItem() instanceof ScaledCloak cloak) {
                        int remaining = stack.getOrCreateTag().getInt(ScaledCloak.CLIMB_TICKS);
                        if (remaining > 0) {
                            stack.getOrCreateTag().putInt(ScaledCloak.CLIMB_TICKS, remaining - 1);
                            player.fallDistance = 0;
                            if (player.tickCount % 20 == 0) {
                                cloak.addExperience(player, stack, 1);
                            }
                        }
                    }
                }
            });
        });
        context.setPacketHandled(true);
    }
}
