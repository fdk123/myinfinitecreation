package fdk123.myinfinitecreation.reliquified;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

public final class HydraFireProjectile extends ThrowableProjectile {
    private int flightTicks = 100;
    private int puddleTicks = 100;
    private float puddleDamage = 1.0f;

    public HydraFireProjectile(EntityType<? extends HydraFireProjectile> type, Level level) {
        super(type, level);
    }

    public void configure(LivingEntity owner, int lifetime, float damage) {
        setOwner(owner);
        flightTicks = Math.max(1, lifetime);
        puddleTicks = Math.max(1, lifetime);
        puddleDamage = Math.max(0, damage);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            for (int i = 0; i < 3; i++) {
                level().addParticle(ParticleTypes.FLAME,
                        getX() + (random.nextDouble() - 0.5) * 0.15,
                        getY() + (random.nextDouble() - 0.5) * 0.15,
                        getZ() + (random.nextDouble() - 0.5) * 0.15,
                        0, 0, 0);
            }
        } else if (--flightTicks <= 0) {
            discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide() || result.getType() == HitResult.Type.MISS || isRemoved()) {
            return;
        }
        HydraFirePuddle puddle = new HydraFirePuddle(HydraFireEntities.PUDDLE.get(), level());
        // On entity impact, anchor the puddle at the entity's feet instead of leaving it suspended.
        double y = result.getType() == HitResult.Type.ENTITY && result instanceof net.minecraft.world.phys.EntityHitResult entityHit
                ? entityHit.getEntity().getY() : result.getLocation().y;
        puddle.moveTo(result.getLocation().x, y, result.getLocation().z);
        puddle.configure(getOwner(), puddleTicks, puddleDamage);
        if (level().addFreshEntity(puddle) && getOwner() instanceof LivingEntity owner) {
            HydraHeart.awardImpactExperience(owner);
        }
        level().playSound(null, blockPosition(), SoundEvents.PUFFER_FISH_BLOW_UP, SoundSource.NEUTRAL,
                0.5f, 1.5f + random.nextFloat() * 0.5f);
        discard();
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FlightTicks", flightTicks);
        tag.putInt("PuddleTicks", puddleTicks);
        tag.putFloat("PuddleDamage", puddleDamage);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        flightTicks = Math.max(1, tag.getInt("FlightTicks"));
        puddleTicks = Math.max(1, tag.getInt("PuddleTicks"));
        puddleDamage = Math.max(0, tag.getFloat("PuddleDamage"));
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
