package net.mebahel.zombiehorde.entity.custom;

import net.mebahel.zombiehorde.entity.ai.ModHordeGoal;
import net.mebahel.zombiehorde.entity.ai.ZombieHordeAttackGoal;
import net.mebahel.zombiehorde.util.ModConfig;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import net.minecraft.entity.data.DataTracker.Builder;

import java.util.List;
import java.util.Objects;

public class ZombieHordeEntity extends ZombieEntity {
    public ZombieHordeEntity(EntityType<? extends ZombieHordeEntity> entityType, World world) {
        super(entityType, world);
        this.setPersistent();
    }

    public void setGlassBreakingProgress(BlockPos pos, int progress) {
        if (this.getWorld() instanceof ServerWorld) {
            ((ServerWorld) this.getWorld()).getServer().getPlayerManager().sendToAll(
                    new BlockBreakingProgressS2CPacket(this.getId(), pos, progress)
            );
        }
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

    @Override
    public void tick() {
        super.tick();
        double maxDistance = 300.0;

        PlayerEntity nearestPlayer = this.getWorld().getClosestPlayer(this, maxDistance);

        if (nearestPlayer == null || this.distanceTo(nearestPlayer) > maxDistance) {
            this.discard();
        }
    }

    protected void initDataTracker(Builder builder) {
        super.initDataTracker(builder);
        builder.add(PATROL_UUID, "");
    }

    @Override
    protected boolean isAffectedByDaylight() {
        if (ModConfig.spawnInDaylight) {
            return false;
        }
        return super.isAffectedByDaylight();
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new BreakDoorGoal(this, zombie -> true));
        this.goalSelector.add(3, new ModHordeGoal(this, 1f, 1f));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.initCustomGoals();
    }



    @Override
    protected void initCustomGoals() {
        this.goalSelector.add(2, new ZombieHordeAttackGoal(this, 1.0, false));
        this.goalSelector.add(6, new MoveThroughVillageGoal(this, 1.0, true, 4, this::canBreakDoors));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));

        this.targetSelector.add(1, (new RevengeGoal(this, new Class[0])).setGroupRevenge(new Class[]{ZombifiedPiglinEntity.class}));
        this.targetSelector.add(2, new ActiveTargetGoal(this, PlayerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal(this, MerchantEntity.class, false));
        this.targetSelector.add(3, new ActiveTargetGoal(this, IronGolemEntity.class, true));
        this.targetSelector.add(5, new ActiveTargetGoal(this, TurtleEntity.class, 10, true, false, TurtleEntity.BABY_TURTLE_ON_LAND_FILTER));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("PatrolUUID", this.getPatrolId().toString());
        nbt.putBoolean("PatrolLeader", this.isPatrolLeader());
        nbt.putBoolean("Patrolling", this.isPatrolling());
        nbt.putBoolean("WasPatrolling", this.wasInitiallyInPatrol());

        if (this.getPatrolTarget() != null) {
            nbt.put("PatrolTarget", NbtHelper.fromBlockPos(this.getPatrolTarget()));
        }
    }
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(PATROL_UUID, nbt.getString("PatrolUUID"));
        this.setPatrolLeader(nbt.getBoolean("PatrolLeader"));
        this.setPatrolling(nbt.getBoolean("Patrolling"));
        this.setWasInitiallyInPatrol(nbt.getBoolean("WasPatrolling"));
        NbtHelper.toBlockPos(nbt, "PatrolTarget").ifPresent(this::setPatrolTarget);
    }

    @Override
    public boolean canBreakDoors() {
        return true;
    }

    public static DefaultAttributeContainer.Builder createZombieAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 35.0)
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0 + ModConfig.hordeMemberBonusHealth)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0)
                .add(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS);
    }
}
