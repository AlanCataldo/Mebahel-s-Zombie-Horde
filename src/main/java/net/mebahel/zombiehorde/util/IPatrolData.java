package net.mebahel.zombiehorde.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;

public interface IPatrolData {
    void setHordeEntityPatrolLeader(boolean isLeader);
    boolean isHordeEntityPatrolLeader();

    void setHordeEntityPatrolTarget(BlockPos target);
    BlockPos getHordeEntityPatrolTarget();

    void setHordeEntityPatrolling(boolean patrolling);
    boolean isHordeEntityPatrolling();

    void setHordeEntityPatrolId(String patrolId);
    String getHordeEntityPatrolId();

    boolean isHordeEntityPartOfSamePatrol(IPatrolData other);

    default void writeToNbt(NbtCompound nbt) {
        nbt.putString("PatrolUUID", this.getHordeEntityPatrolId());
        nbt.putBoolean("PatrolLeader", this.isHordeEntityPatrolLeader());
        nbt.putBoolean("Patrolling", this.isHordeEntityPatrolling());

        BlockPos target = this.getHordeEntityPatrolTarget();
        if (target != null) {
            nbt.put("PatrolTarget", NbtHelper.fromBlockPos(target));
        }
    }

    default void readFromNbt(NbtCompound nbt) {
        this.setHordeEntityPatrolId(nbt.getString("PatrolUUID"));
        this.setHordeEntityPatrolLeader(nbt.getBoolean("PatrolLeader"));
        this.setHordeEntityPatrolling(nbt.getBoolean("Patrolling"));
        NbtHelper.toBlockPos(nbt, "PatrolTarget").ifPresent(this::setHordeEntityPatrolTarget);
    }
}