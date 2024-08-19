package net.mebahel.zombiehorde.entity.custom;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class ZombieHordeEntity extends ZombieEntity {
    public ZombieHordeEntity(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
    }
    @Nullable
    private BlockPos patrolTarget;
    private boolean patrolLeader;
    private boolean patrolling;
    private boolean wasInitiallyInPatrol = false;
    public static final TrackedData<String> PATROL_UUID = DataTracker.registerData(ZombieHordeEntity.class,
            TrackedDataHandlerRegistry.STRING);

    public void setPatrolTarget(BlockPos targetPos) {
        this.patrolTarget = targetPos;
        this.patrolling = true;
    }

    public @Nullable BlockPos getPatrolTarget() {
        return this.patrolTarget;
    }

    public void setPatrolLeader(boolean patrolLeader) {
        this.patrolLeader = patrolLeader;
        this.patrolling = true;
    }

    public boolean isPatrolLeader() {
        return this.patrolLeader;
    }

    public boolean isPatrolling() {
        return this.patrolling && this.getTarget() == null;
    }

    public boolean wasInitiallyInPatrol() {
        return this.wasInitiallyInPatrol;
    }

    public void setWasInitiallyInPatrol(boolean wasInPatrol) {
        this.wasInitiallyInPatrol = wasInPatrol;
    }

    public boolean isAnyMemberAttacking() {
        List<ZombieHordeEntity> patrolMembers = this.getWorld().getEntitiesByClass(ZombieHordeEntity.class, this.getBoundingBox().expand(16.0), e -> e.isPartOf(this));
        for (ZombieHordeEntity member : patrolMembers) {
            if (member.getTarget() != null) {
                return true;
            }
        }
        return false;
    }

    public void setPatrolling(boolean b) {
        this.patrolling = b;
    }

    public void checkAndResumePatrolling() {
        if (!this.isAnyMemberAttacking()) {
            this.setPatrolling(true);
        }
    }

    public String getPatrolId() {
        return this.dataTracker.get(PATROL_UUID);
    }

    public void setPatrolId(String patrolId) {
        this.dataTracker.set(PATROL_UUID, patrolId);
    }

    public boolean isPartOfSamePatrol(ZombieHordeEntity other) {
        return Objects.equals(this.dataTracker.get(PATROL_UUID), other.getPatrolId());
    }
}
