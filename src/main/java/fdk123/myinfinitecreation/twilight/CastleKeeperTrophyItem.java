package fdk123.myinfinitecreation.twilight;

import fdk123.myinfinitecreation.client.CastleKeeperTrophyItemRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.ModList;

import java.util.function.Consumer;

public final class CastleKeeperTrophyItem extends BlockItem {
    public CastleKeeperTrophyItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot slot, Entity entity) {
        return slot == EquipmentSlot.HEAD;
    }

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.HEAD;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        EquipmentSlot slot = Mob.getEquipmentSlotForItem(held);
        if (slot == EquipmentSlot.HEAD && player.getItemBySlot(slot).isEmpty()) {
            player.setItemSlot(slot, held.split(1));
            return InteractionResultHolder.sidedSuccess(held, level.isClientSide());
        }
        return InteractionResultHolder.fail(held);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) {
        if (ModList.get().isLoaded("curios")) {
            return CastleKeeperCuriosCompat.createProvider(stack);
        }
        return super.initCapabilities(stack, nbt);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private CastleKeeperTrophyItemRenderer renderer;

            @Override
            public CastleKeeperTrophyItemRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new CastleKeeperTrophyItemRenderer();
                }
                return renderer;
            }
        });
    }
}
