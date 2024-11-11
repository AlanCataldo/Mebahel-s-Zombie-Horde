package net.mebahel.zombiehorde.entity.ai;

import net.mebahel.zombiehorde.util.IPatrolData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class ModHordeGoal extends Goal {
    private final MobEntity entity;
    private final double baseSpeed;
    private final double leaderSpeed;
    private final double followSpeed;
    private MobEntity assignedLeader;

    public ModHordeGoal(MobEntity entity, double leaderSpeed, double followSpeed) {
        if (!(entity instanceof IPatrolData)) {
            throw new IllegalArgumentException("Entity must implement IPatrolData");
        }
        this.entity = entity;
        this.baseSpeed = Objects.requireNonNull(this.entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)).getBaseValue();
        this.leaderSpeed = leaderSpeed;
        this.followSpeed = followSpeed;
        this.setControls(EnumSet.of(Control.MOVE));
        this.assignedLeader = null;
    }

    @Override
    public void start() {
        super.start();
        Objects.requireNonNull(this.entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.27D);
    }

    @Override
    public void stop() {
        super.stop();
        Objects.requireNonNull(this.entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)).setBaseValue(this.baseSpeed);
    }

    @Override
    public boolean canStart() {
        IPatrolData patrolData = (IPatrolData) entity;

        // Check if the entity is patrolling
        if (!patrolData.isHordeEntityPatrolling()) {
            return false;
        }

        // If this entity or any horde member has a target, we want to start
        if (entity.getTarget() != null) {
            shareTargetWithHorde(entity.getTarget());
            return false; // No need to start this goal if we already have a target
        }

        // If the assigned leader is invalid, try to find a new one
        if (this.assignedLeader == null || !this.assignedLeader.isAlive()) {
            this.assignedLeader = findNewLeader();
        }

        return this.assignedLeader != null && this.assignedLeader.isAlive();
    }

    @Override
    public boolean shouldContinue() {
        // Continue patrolling if we don't have a target
        if (this.entity.getTarget() != null) {
            shareTargetWithHorde(this.entity.getTarget());
            this.stop();
            return false; // No need to start this goal if we already have a target
        }
        return this.entity.getTarget() == null;
    }

    @Override
    public void tick() {
        boolean hasNearbyPlayer = this.entity.getWorld().getPlayers().stream()
                .anyMatch(player -> player.squaredDistanceTo(this.entity.getX(), this.entity.getY(), this.entity.getZ()) <= 60000);

        // If no players are nearby, discard the entity
        if (!hasNearbyPlayer && this.entity.age > 20) {
            entity.discard();
            return; // Stop further execution of tick if entity is discarded
        }
        IPatrolData patrolData = (IPatrolData) entity;
        EntityNavigation navigation = this.entity.getNavigation();

        if (navigation.isIdle()) {
            // Ensure the leader is valid
            if (this.assignedLeader == null || !this.assignedLeader.isAlive()) {
                this.assignedLeader = findNewLeader();
            }

            // If a target is acquired, share it with the horde
            LivingEntity currentTarget = this.assignedLeader != null ? this.assignedLeader.getTarget() : null;
            if (currentTarget != null) {
                shareTargetWithHorde(currentTarget);
                return; // No need to continue patrolling if we have a target
            }

            // Set patrol target and navigate if there's no target
            BlockPos targetPos = this.assignedLeader != null ? ((IPatrolData) assignedLeader).getHordeEntityPatrolTarget() : null;
            if (!patrolData.isHordeEntityPatrolLeader() && targetPos != null && !targetPos.equals(patrolData.getHordeEntityPatrolTarget())) {
                patrolData.setHordeEntityPatrolTarget(targetPos);
            }

            if (targetPos != null) {
                Path path = navigation.findPathTo(targetPos, 0);
                if (path != null) {
                    navigation.startMovingAlong(path, patrolData.isHordeEntityPatrolLeader() ? leaderSpeed : followSpeed);
                } else if (patrolData.isHordeEntityPatrolLeader()) {
                    BlockPos newTarget = setRandomPatrolTarget((ServerWorld) entity.getWorld(), entity.getBlockPos());
                    patrolData.setHordeEntityPatrolTarget(newTarget);
                }
            } else {
                this.wander();
            }
        }
    }

    public static BlockPos setRandomPatrolTarget(ServerWorld world, BlockPos pos) {
        Random random = world.random;
        int x = 150 + random.nextInt(150);
        int z = 150 + random.nextInt(150);

        if (random.nextBoolean()) x = -x;
        if (random.nextBoolean()) z = -z;

        BlockPos roughTargetPos = new BlockPos(pos.getX() + x, world.getHeight(), pos.getZ() + z);
        return findTopSolidBlock(world, roughTargetPos);
    }

    private static BlockPos findTopSolidBlock(ServerWorld world, BlockPos pos) {
        return world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos);
    }

    private MobEntity findNewLeader() {
        List<MobEntity> patrolMembers = entity.getWorld().getEntitiesByClass(
                MobEntity.class, entity.getBoundingBox().expand(64.0),
                e -> e instanceof IPatrolData && ((IPatrolData) e).isHordeEntityPatrolling() && ((IPatrolData) e).isHordeEntityPartOfSamePatrol((IPatrolData) entity)
        );

        for (MobEntity member : patrolMembers) {
            if (member.isAlive() && ((IPatrolData) member).isHordeEntityPatrolLeader()) {
                return member;
            }
        }

        for (MobEntity member : patrolMembers) {
            if (member.isAlive() && !((IPatrolData) member).isHordeEntityPatrolLeader()) {
                ((IPatrolData) member).setHordeEntityPatrolLeader(true);
                return member;
            }
        }

        return null;
    }

    private void shareTargetWithHorde(LivingEntity target) {
        List<MobEntity> patrolMembers = entity.getWorld().getEntitiesByClass(
                MobEntity.class, entity.getBoundingBox().expand(64.0),
                e -> e instanceof IPatrolData && ((IPatrolData) e).isHordeEntityPatrolling() && ((IPatrolData) e).isHordeEntityPartOfSamePatrol((IPatrolData) entity)
        );

        for (MobEntity member : patrolMembers) {
            if (member.getTarget() == null) {
                member.setTarget(target); // Share the target
            }
        }
    }

    private void wander() {
        Random random = this.entity.getRandom();
        BlockPos blockPos = this.entity.getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                this.entity.getBlockPos().add(-8 + random.nextInt(16), 0, -8 + random.nextInt(16)));
        this.entity.getNavigation().startMovingTo(blockPos.getX(), blockPos.getY(), blockPos.getZ(), this.leaderSpeed);
    }
}