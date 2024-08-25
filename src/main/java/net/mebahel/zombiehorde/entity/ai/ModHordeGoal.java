package net.mebahel.zombiehorde.entity.ai;

import net.mebahel.zombiehorde.entity.custom.ZombieHordeEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
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

        if (entityNavigation.isIdle()) {
            BlockPos targetPos = this.entity.isPatrolLeader() ? this.entity.getPatrolTarget() :
                    this.assignedLeader != null ? this.assignedLeader.getPatrolTarget() : null;

            if (targetPos != null) {
                Path path = entityNavigation.findPathTo(targetPos, 0);
                if (path != null) {
                    entityNavigation.startMovingAlong(path, this.entity.isPatrolLeader() ? leaderSpeed : followSpeed);
                } else {
                    this.wander();
                }
            } else {
                this.wander();
            }
        }
    }

    private ZombieHordeEntity findNewLeader() {
        // Trouver tous les membres de la patrouille
        List<ZombieHordeEntity> patrolMembers = this.entity.getWorld().getEntitiesByClass(ZombieHordeEntity.class, this.entity.getBoundingBox().expand(32.0), e -> e.isPartOfSamePatrol(this.entity));

        // Vérifier s'il y a déjà un leader
        for (ZombieHordeEntity member : patrolMembers) {
            if (!member.isDead() && member.isPatrolLeader()) {
                return member;
            }
        }

        // Si aucun autre leader n'existe, promouvoir le premier membre non mort qui n'est pas déjà un leader
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
