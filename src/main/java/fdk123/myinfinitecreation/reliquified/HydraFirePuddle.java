package fdk123.myinfinitecreation.reliquified;

import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public final class HydraFirePuddle extends Entity {
    private static final EntityDataAccessor<Float> DAMAGE =
            SynchedEntityData.defineId(HydraFirePuddle.class, EntityDataSerializers.FLOAT);

    private int lifetime = 100;
    private UUID ownerId;
    private Entity cachedOwner;

    public HydraFirePuddle(EntityType<? extends HydraFirePuddle> type, Level level) {
        super(type, level);
    }

    public void configure(Entity owner, int ticks, float damage) {
        ownerId = owner == null ? null : owner.getUUID();
        cachedOwner = owner;
        lifetime = Math.max(1, ticks);
        entityData.set(DAMAGE, Math.max(0, damage));
    }

    private Entity owner() {
        if (cachedOwner != null && !cachedOwner.isRemoved()) {
            return cachedOwner;
        }
        if (ownerId != null && level() instanceof ServerLevel serverLevel) {
            cachedOwner = serverLevel.getEntity(ownerId);
        }
        return cachedOwner;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            for (int i = 0; i < 16; i++) {
                double angle = (i * Math.PI / 8) + tickCount * 0.04;
                level().addParticle(ParticleTypes.FLAME,
                        getX() + Math.cos(angle) * 1.95,
                        getY() + 0.05,
                        getZ() + Math.sin(angle) * 1.95,
                        0, 0.01, 0);
            }
            if (random.nextFloat() < 0.25f) {
                level().addParticle(ParticleTypes.LAVA,
                        getX() + (random.nextDouble() - 0.5) * 3.5,
                        getY() + 0.05,
                        getZ() + (random.nextDouble() - 0.5) * 3.5,
                        0, 0, 0);
            }
            return;
        }

        if (lifetime-- <= 0) {
            discard();
            return;
        }
        Entity source = owner();
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(0, 0.1, 0), candidate -> candidate.isAlive()
                        && candidate != source
                        && (source == null || !EntityUtils.isAlliedTo(source, candidate)))) {
            target.hurt(new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.IN_FIRE), this, source), entityData.get(DAMAGE));
            target.setRemainingFireTicks(100);
        }
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DAMAGE, 1.0f);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        lifetime = Math.max(1, tag.getInt("Lifetime"));
        entityData.set(DAMAGE, Math.max(0, tag.getFloat("Damage")));
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        cachedOwner = null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Lifetime", lifetime);
        tag.putFloat("Damage", entityData.get(DAMAGE));
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
