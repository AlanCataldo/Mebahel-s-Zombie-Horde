package net.mebahel.zombiehorde.entity.ai;

import net.mebahel.zombiehorde.entity.custom.ZombieHordeEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

import java.util.EnumSet;
import java.util.List;

public class ModHordeGoal extends Goal {
    private final ZombieHordeEntity entity;
    private final double leaderSpeed;
    private final double followSpeed;
    private ZombieHordeEntity assignedLeader;

    public ModHordeGoal(ZombieHordeEntity entity, double leaderSpeed, double followSpeed) {
        this.entity = entity;
        this.leaderSpeed = leaderSpeed;
        this.followSpeed = followSpeed;
        this.setControls(EnumSet.of(Control.MOVE));
        this.assignedLeader = null;
    }

    @Override
    public boolean canStart() {
        if (!this.entity.isPatrolling()) {
            return false;
        }

        // Vérifier si le leader assigné est toujours en vie, sinon désigner un nouveau leader
        if (this.assignedLeader == null || this.assignedLeader.isDead()) {
            this.assignedLeader = findNewLeader();
        }

        // Ne démarre que si un leader valide existe
        return this.assignedLeader != null && !this.assignedLeader.isDead();
    }

    @Override
    public void tick() {
        EntityNavigation entityNavigation = this.entity.getNavigation();

        // Check if the entity is idle (not currently moving)
        if (entityNavigation.isIdle()) {
            // Ensure the assigned leader is valid
            if (this.assignedLeader == null || this.assignedLeader.isDead()) {
                this.assignedLeader = findNewLeader();
            }

            // Get the target position from the leader
            BlockPos targetPos = this.assignedLeader != null ? this.assignedLeader.getPatrolTarget() : null;

            // If this entity is not the leader, ensure it has the same patrol target as the leader
            if (!this.entity.isPatrolLeader() && targetPos != null && !targetPos.equals(this.entity.getPatrolTarget())) {
                this.entity.setPatrolTarget(targetPos);
            }

            // If there's a valid target position, attempt to pathfind to it
            if (targetPos != null) {
                Path path = entityNavigation.findPathTo(targetPos, 0);

                if (path != null) {
                    entityNavigation.startMovingAlong(path, this.entity.isPatrolLeader() ? leaderSpeed : followSpeed);
                } else {
                    // Only the leader should recalculate the patrol target if pathfinding fails
                    if (this.entity.isPatrolLeader()) {
                        BlockPos newTarget = setRandomPatrolTarget((ServerWorld) this.entity.getWorld(), this.entity.getBlockPos());
                        this.entity.setPatrolTarget(newTarget);
                    }
                }
            } else {
                this.wander();
            }
        }
    }
    public static BlockPos setRandomPatrolTarget(ServerWorld world, BlockPos pos) {
        int x = 150 + world.random.nextInt(150);
        int z = 150 + world.random.nextInt(150);

        if (world.random.nextBoolean()) x = -x;
        if (world.random.nextBoolean()) z = -z;

        BlockPos roughTargetPos = new BlockPos(pos.getX() + x, world.getHeight(), pos.getZ() + z);

        BlockPos finalTargetPos = findTopSolidBlock(world, roughTargetPos);

        if (finalTargetPos.getY() < world.getBottomY() || finalTargetPos.getY() > world.getTopY()) {
            finalTargetPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos);
        }

        return finalTargetPos;
    }
    private static BlockPos findTopSolidBlock(ServerWorld world, BlockPos pos) {
        return world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos);
    }


    private ZombieHordeEntity findNewLeader() {
        List<ZombieHordeEntity> patrolMembers = this.entity.getWorld().getEntitiesByClass(ZombieHordeEntity.class, this.entity.getBoundingBox().expand(64.0), e -> e.isPartOfSamePatrol(this.entity));

        for (ZombieHordeEntity member : patrolMembers) {
            if (!member.isDead() && member.isPatrolLeader()) {
                return member;
            }
        }

        for (ZombieHordeEntity member : patrolMembers) {
            if (!member.isDead() && !member.isPatrolLeader()) {
                member.setPatrolLeader(true);
                return member;
            }
        }

        return null;
    }

    private void wander() {
        Random random = this.entity.getRandom();
        BlockPos blockPos = this.entity.getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, this.entity.getBlockPos().add(-8 + random.nextInt(16), 0, -8 + random.nextInt(16)));

        this.entity.getNavigation().startMovingTo(blockPos.getX(), blockPos.getY(), blockPos.getZ(), this.leaderSpeed);
    }
}
