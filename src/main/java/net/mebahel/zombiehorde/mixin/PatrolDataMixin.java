package net.mebahel.zombiehorde.mixin;

import net.mebahel.zombiehorde.util.IPatrolData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class PatrolDataMixin implements IPatrolData {
    @Shadow
    protected DataTracker dataTracker;

    private static final TrackedData<Boolean> HORDE_PATROL_LEADER = DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<BlockPos> HORDE_PATROL_TARGET = DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.BLOCK_POS);
    private static final TrackedData<Boolean> HORDE_PATROLLING = DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<String> HORDE_PATROL_ID = DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.STRING);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initPatrolData(CallbackInfo ci) {
        dataTracker.startTracking(HORDE_PATROL_LEADER, false);
        dataTracker.startTracking(HORDE_PATROL_TARGET, BlockPos.ORIGIN);
        dataTracker.startTracking(HORDE_PATROLLING, false);
        dataTracker.startTracking(HORDE_PATROL_ID, ""); // Initialise avec un ID de patrouille vide
    }

    @Override
    public void setHordeEntityPatrolLeader(boolean isLeader) {
        this.dataTracker.set(HORDE_PATROL_LEADER, isLeader);
    }

    @Override
    public boolean isHordeEntityPatrolLeader() {
        return this.dataTracker.get(HORDE_PATROL_LEADER);
    }

    @Override
    public void setHordeEntityPatrolTarget(BlockPos target) {
        this.dataTracker.set(HORDE_PATROL_TARGET, target);
    }

    @Override
    public BlockPos getHordeEntityPatrolTarget() {
        return this.dataTracker.get(HORDE_PATROL_TARGET);
    }

    @Override
    public void setHordeEntityPatrolling(boolean patrolling) {
        this.dataTracker.set(HORDE_PATROLLING, patrolling);
    }

    @Override
    public boolean isHordeEntityPatrolling() {
        return this.dataTracker.get(HORDE_PATROLLING);
    }

    @Override
    public void setHordeEntityPatrolId(String patrolId) {
        this.dataTracker.set(HORDE_PATROL_ID, patrolId);
    }

    @Override
    public String getHordeEntityPatrolId() {
        return this.dataTracker.get(HORDE_PATROL_ID);
    }

    @Override
    public boolean isHordeEntityPartOfSamePatrol(IPatrolData other) {
        return this.getHordeEntityPatrolId().equals(other.getHordeEntityPatrolId());
    }
}