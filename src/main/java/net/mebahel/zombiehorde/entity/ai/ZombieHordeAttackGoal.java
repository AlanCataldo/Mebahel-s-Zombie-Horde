package net.mebahel.zombiehorde.entity.ai;

import net.mebahel.zombiehorde.entity.custom.ZombieHordeEntity;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.HashMap;
import java.util.Map;

public class ZombieHordeAttackGoal extends MeleeAttackGoal {
    private final ZombieHordeEntity zombie;
    private int ticks;
    private Path path;
    private long lastUpdateTime;
    private BlockPos targetGlassBlockPos = null;
    private int glassBreakingProgress = 0;
    private static final int MAX_GLASS_BREAKING_PROGRESS = 20; // Nombre de ticks nécessaires pour casser le verre

    // Map pour stocker la progression de la destruction des blocs de verre
    private static final Map<BlockPos, Integer> glassBreakingProgressMap = new HashMap<>();

    public ZombieHordeAttackGoal(ZombieHordeEntity zombie, double speed, boolean pauseWhenMobIdle) {
        super(zombie, speed, pauseWhenMobIdle);
        this.zombie = zombie;
    }

    @Override
    public void start() {
        super.start();
        this.ticks = 0;

        // Recherche d'une cible à proximité qui pourrait être derrière du verre
        LivingEntity target = findTargetThroughGlass();
        if (target != null) {
            this.zombie.setTarget(target);
        }

        List<ZombieHordeEntity> patrolMembers = this.zombie.getWorld().getEntitiesByClass(ZombieHordeEntity.class, this.zombie.getBoundingBox().expand(32.0), e -> e.isPartOfSamePatrol(this.zombie));
        for (ZombieHordeEntity member : patrolMembers) {
            if (member.isPatrolling()) {
                member.setPatrolling(false);
                member.setTarget(target);
            }
        }
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity livingEntity = this.mob.getTarget();

        if (livingEntity instanceof PlayerEntity) {
            PlayerEntity playerEntity = (PlayerEntity) livingEntity;
            if (playerEntity.isCreative() || playerEntity.isSpectator()) {
                return false;
            }
        }
        return livingEntity != null && livingEntity.isAlive();
    }

    @Override
    public void stop() {
        super.stop();
        this.zombie.setAttacking(false);
        ZombieHordeEntity patrolEntity = (ZombieHordeEntity) this.mob;
        if (patrolEntity.wasInitiallyInPatrol()) {
            patrolEntity.checkAndResumePatrolling();
        }
        // Réinitialiser la progression de casse du verre
        resetBlockBreakingProgress();
    }

    @Override
    public void tick() {
        super.tick();
        ++this.ticks;

        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            if (this.ticks % 20 == 0) {  // Vérifier toutes les secondes
                this.path = this.zombie.getNavigation().findPathTo(target, 1);
                if (this.path != null) {
                    BlockPos glassBlockPos = findGlassOnPath(this.path);

                    if (glassBlockPos != null) {
                        if (this.targetGlassBlockPos == null || !this.targetGlassBlockPos.equals(glassBlockPos)) {
                            resetBlockBreakingProgress();  // Réinitialiser quand la cible change
                            this.targetGlassBlockPos = glassBlockPos;
                            this.glassBreakingProgress = 0;
                        }

                        // Gérer la casse progressive du verre
                        this.glassBreakingProgress = glassBreakingProgressMap.getOrDefault(this.targetGlassBlockPos, 0) + 1;

                        if (this.glassBreakingProgress >= MAX_GLASS_BREAKING_PROGRESS) {
                            ((ServerWorld) this.zombie.getWorld()).breakBlock(this.targetGlassBlockPos, true, this.zombie);
                            glassBreakingProgressMap.remove(this.targetGlassBlockPos);
                            resetBlockBreakingProgress();  // Réinitialiser après la destruction
                        } else {
                            glassBreakingProgressMap.put(this.targetGlassBlockPos, this.glassBreakingProgress);
                            this.zombie.swingHand(this.zombie.getActiveHand());
                            this.zombie.setGlassBreakingProgress(this.targetGlassBlockPos, this.glassBreakingProgress * 10 / MAX_GLASS_BREAKING_PROGRESS);
                        }
                    } else {
                        this.zombie.getNavigation().startMovingAlong(this.path, 1);
                    }
                }
            }

            // Continuer à casser le bloc de verre s'il est encore là
            if (this.targetGlassBlockPos != null && isGlassBlock(this.targetGlassBlockPos)) {
                this.glassBreakingProgress = glassBreakingProgressMap.getOrDefault(this.targetGlassBlockPos, 0) + 1;

                if (this.glassBreakingProgress >= MAX_GLASS_BREAKING_PROGRESS) {
                    ((ServerWorld) this.zombie.getWorld()).breakBlock(this.targetGlassBlockPos, true, this.zombie);
                    glassBreakingProgressMap.remove(this.targetGlassBlockPos);
                    resetBlockBreakingProgress();  // Réinitialiser après la destruction
                } else {
                    glassBreakingProgressMap.put(this.targetGlassBlockPos, this.glassBreakingProgress);
                    this.zombie.swingHand(this.zombie.getActiveHand());
                    this.zombie.setGlassBreakingProgress(this.targetGlassBlockPos, this.glassBreakingProgress * 10 / MAX_GLASS_BREAKING_PROGRESS);
                }
            } else {
                this.glassBreakingProgress = 0;
            }

            checkAndBreakGlassInFront();
        }
    }

    private void resetBlockBreakingProgress() {
        if (this.targetGlassBlockPos != null) {
            ((ServerWorld) this.zombie.getWorld()).setBlockBreakingInfo(this.zombie.getId(), this.targetGlassBlockPos, -1);
            glassBreakingProgressMap.remove(this.targetGlassBlockPos);
            this.targetGlassBlockPos = null;
            this.glassBreakingProgress = 0;
        }
    }

    private void checkAndBreakGlassInFront() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }

        // Position du zombie au niveau des yeux, et avec un décalage vertical (pour gérer les sauts)
        Vec3d zombieEyePos = this.zombie.getEyePos().add(0, 1, 0); // Décalage vertical de +1
        Vec3d targetPos = target.getEyePos();

        // Raycast pour détecter les blocs entre le zombie et la cible, y compris ceux au-dessus de la hauteur des yeux
        RaycastContext context = new RaycastContext(zombieEyePos, targetPos, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, this.zombie);
        BlockPos hitPos = this.zombie.getWorld().raycast(context).getBlockPos();

        BlockPos blockInFrontPos = this.zombie.getBlockPos().offset(this.zombie.getMovementDirection());

        // Si le raycast ne touche pas les blocs en face du zombie, ne rien faire
        if (!hitPos.equals(blockInFrontPos) && !hitPos.equals(blockInFrontPos.up()) && !hitPos.equals(blockInFrontPos.up(2))) {
            return;
        }

        // Vérifier les positions à la hauteur des pieds, des yeux, et au-dessus
        for (int i = 0; i <= 2; i++) { // Parcours des trois hauteurs possibles (pieds, yeux, au-dessus)
            BlockPos currentPos = blockInFrontPos.up(i);
            if (isGlassBlock(currentPos)) {
                this.targetGlassBlockPos = currentPos;
                this.glassBreakingProgress = 0;
                break;
            }
        }

        // Si un bloc de verre est identifié, commencer à le casser
        if (this.targetGlassBlockPos != null && isGlassBlock(this.targetGlassBlockPos)) {
            this.glassBreakingProgress = glassBreakingProgressMap.getOrDefault(this.targetGlassBlockPos, 0) + 1;

            if (this.glassBreakingProgress >= MAX_GLASS_BREAKING_PROGRESS) {
                ((ServerWorld) this.zombie.getWorld()).breakBlock(this.targetGlassBlockPos, true, this.zombie);
                glassBreakingProgressMap.remove(this.targetGlassBlockPos);
                resetBlockBreakingProgress(); // Assurez-vous que l'état de casse est réinitialisé après destruction
            } else {
                glassBreakingProgressMap.put(this.targetGlassBlockPos, this.glassBreakingProgress);
                this.zombie.swingHand(this.zombie.getActiveHand());
                this.zombie.setGlassBreakingProgress(this.targetGlassBlockPos, this.glassBreakingProgress * 10 / MAX_GLASS_BREAKING_PROGRESS);
            }
        }
    }

    private boolean isGlassBlock(BlockPos pos) {
        Block block = this.zombie.getWorld().getBlockState(pos).getBlock();
        return block == Blocks.GLASS || block == Blocks.GLASS_PANE;
    }

    private BlockPos findGlassOnPath(Path path) {
        for (int i = 0; i < path.getLength(); i++) {
            BlockPos pos = path.getNodePos(i);
            Block block = this.zombie.getWorld().getBlockState(pos).getBlock();

            if (block == Blocks.GLASS || block == Blocks.GLASS_PANE) {
                return pos;
            }
        }
        return null;
    }

    private LivingEntity findTargetThroughGlass() {
        Box detectionBox = this.zombie.getBoundingBox().expand(16.0);
        List<LivingEntity> potentialTargets = this.zombie.getWorld().getEntitiesByClass(LivingEntity.class, detectionBox, entity -> entity instanceof PlayerEntity);

        for (LivingEntity potentialTarget : potentialTargets) {
            if (this.canSeeThroughGlass(potentialTarget)) {
                return potentialTarget;
            }
        }

        return null;
    }

    private boolean canSeeThroughGlass(LivingEntity target) {
        Vec3d startPos = this.zombie.getEyePos();
        Vec3d endPos = target.getEyePos();
        RaycastContext context = new RaycastContext(startPos, endPos, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, this.zombie);
        BlockPos hitPos = this.zombie.getWorld().raycast(context).getBlockPos();
        Block hitBlock = this.zombie.getWorld().getBlockState(hitPos).getBlock();

        return hitBlock == Blocks.GLASS || hitBlock == Blocks.GLASS_PANE;
    }

    @Override
    public boolean canStart() {
        long currentTime = this.mob.getWorld().getTime();

        if (currentTime - this.lastUpdateTime < 20L) {
            return false;
        }

        this.lastUpdateTime = currentTime;

        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            target = findTargetThroughGlass();
            if (target != null) {
                this.mob.setTarget(target);
            } else {
                return false;
            }
        }

        this.path = this.mob.getNavigation().findPathTo(target, 1);
        return this.path != null || this.getSquaredMaxAttackDistance(target) >= this.mob.squaredDistanceTo(target.getX(), target.getY(), target.getZ());
    }
}