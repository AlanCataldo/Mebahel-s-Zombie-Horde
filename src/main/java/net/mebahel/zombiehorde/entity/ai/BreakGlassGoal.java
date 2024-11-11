package net.mebahel.zombiehorde.entity.ai;

import net.mebahel.zombiehorde.util.IPatrolData;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BreakGlassGoal extends Goal {
    private final MobEntity zombie;
    private final boolean hordeMemberBreakFence;
    private final boolean hordeMemberBreakGlass;
    private BlockPos targetGlassBlockPos = null;
    private int glassBreakingProgress = 0;
    private int tickCounter = 0; // Compteur de ticks
    private static final int MAX_GLASS_BREAKING_PROGRESS = 10; // Reduced for quicker breaking
    private static final Map<BlockPos, Integer> glassBreakingProgressMap = new HashMap<>();

    public BreakGlassGoal(MobEntity zombie,boolean hordeMemberBreakGlass, boolean hordeMemberBreakFence) {
        this.zombie = zombie;
        this.hordeMemberBreakGlass = hordeMemberBreakGlass;
        this.hordeMemberBreakFence = hordeMemberBreakFence; // Initialisation
    }

    @Override
    public boolean canStart() {
        // Check if the zombie is part of a patrol
        if (!isPartOfPatrol()) {
            return false;
        }

        LivingEntity target = this.zombie.getTarget();
        if (target == null || !target.isAlive()) {
            target = findTargetThroughGlass();
            if (target != null) {
                this.zombie.setTarget(target);
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity livingEntity = this.zombie.getTarget();
        return livingEntity != null && livingEntity.isAlive() &&
                !(livingEntity instanceof PlayerEntity && (((PlayerEntity) livingEntity).isCreative() || ((PlayerEntity) livingEntity).isSpectator()));
    }

    @Override
    public void start() {
        LivingEntity target = findTargetThroughGlass();
        if (target != null) {
            this.zombie.setTarget(target);
        }
    }

    @Override
    public void stop() {
        resetBlockBreakingProgress();
    }

    @Override
    public void tick() {
        tickCounter++; // Incrémente le compteur de ticks

        // Effectue la vérification seulement toutes les 20 ticks (1 seconde)
        if (tickCounter >= 20) {
            tickCounter = 0; // Réinitialise le compteur

            // Cherche une entité derrière un bloc de verre ou de fence
            LivingEntity target = findTargetThroughGlass();
            if (target != null) {
                this.zombie.setTarget(target);
            }
        }

        // Cherche le bloc de verre ou de fence le plus proche devant le zombie
        BlockPos glassBlockPos = findGlassBlockInFrontTopToBottom();
        if (glassBlockPos != null) {
            if (!glassBlockPos.equals(this.targetGlassBlockPos)) {
                resetBlockBreakingProgress();
                this.targetGlassBlockPos = glassBlockPos;
                this.glassBreakingProgress = 0;
            }

            // Incrémente la progression de casse chaque tick
            this.glassBreakingProgress = glassBreakingProgressMap.getOrDefault(this.targetGlassBlockPos, 0) + 1;
            if (this.glassBreakingProgress >= MAX_GLASS_BREAKING_PROGRESS) {
                if (this.zombie.getWorld() instanceof ServerWorld) {
                    ((ServerWorld) this.zombie.getWorld()).breakBlock(this.targetGlassBlockPos, true, this.zombie);
                }
                resetBlockBreakingProgress();
            } else {
                glassBreakingProgressMap.put(this.targetGlassBlockPos, this.glassBreakingProgress);
                this.zombie.swingHand(this.zombie.getActiveHand());
                setGlassBreakingProgress(this.targetGlassBlockPos, this.glassBreakingProgress * 10 / MAX_GLASS_BREAKING_PROGRESS);
            }
        }
    }

    private void resetBlockBreakingProgress() {
        if (this.targetGlassBlockPos != null) {
            if (this.zombie.getWorld() instanceof ServerWorld) {
                ((ServerWorld) this.zombie.getWorld()).getServer().getPlayerManager().sendToAll(
                        new BlockBreakingProgressS2CPacket(this.zombie.getId(), this.targetGlassBlockPos, -1)
                );
            }
            glassBreakingProgressMap.remove(this.targetGlassBlockPos);
            this.targetGlassBlockPos = null;
            this.glassBreakingProgress = 0;
        }
    }

    private BlockPos findGlassBlockInFrontTopToBottom() {
        BlockPos currentPos = this.zombie.getBlockPos();
        Direction facing = this.zombie.getMovementDirection(); // Get the direction the zombie is facing

        // Check up to 10 blocks in the direction the zombie is facing, from top to bottom
        for (int y = 2; y >= -2; y--) { // Start from 2 blocks above and go down to 2 blocks below
            for (int i = 1; i <= 2; i++) {
                BlockPos pos = currentPos.offset(facing, i).up(y);
                if (isGlassOrBreakableBlock(pos)) { // Utilisation de la nouvelle méthode
                    return pos;
                }
            }
        }
        return null;
    }

    private boolean isGlassOrBreakableBlock(BlockPos pos) {
        Block block = this.zombie.getWorld().getBlockState(pos).getBlock();
        if (hordeMemberBreakGlass && hordeMemberBreakFence) {
            return block == Blocks.GLASS || block == Blocks.GLASS_PANE || block == Blocks.OAK_FENCE || block == Blocks.SPRUCE_FENCE || block == Blocks.BIRCH_FENCE ||
                    block == Blocks.JUNGLE_FENCE || block == Blocks.ACACIA_FENCE || block == Blocks.DARK_OAK_FENCE ||
                    block == Blocks.MANGROVE_FENCE ||
                    block == Blocks.OAK_FENCE_GATE || block == Blocks.SPRUCE_FENCE_GATE || block == Blocks.BIRCH_FENCE_GATE ||
                    block == Blocks.JUNGLE_FENCE_GATE || block == Blocks.ACACIA_FENCE_GATE || block == Blocks.DARK_OAK_FENCE_GATE ||
                    block == Blocks.MANGROVE_FENCE_GATE;
        } else if (hordeMemberBreakGlass && !hordeMemberBreakFence) {
            return block == Blocks.GLASS || block == Blocks.GLASS_PANE;
        } else if (!hordeMemberBreakGlass && hordeMemberBreakFence) {
            // Vérifie si c'est un fence ou une porte de fence
            return block == Blocks.OAK_FENCE || block == Blocks.SPRUCE_FENCE || block == Blocks.BIRCH_FENCE ||
                    block == Blocks.JUNGLE_FENCE || block == Blocks.ACACIA_FENCE || block == Blocks.DARK_OAK_FENCE ||
                    block == Blocks.MANGROVE_FENCE ||
                    block == Blocks.OAK_FENCE_GATE || block == Blocks.SPRUCE_FENCE_GATE || block == Blocks.BIRCH_FENCE_GATE ||
                    block == Blocks.JUNGLE_FENCE_GATE || block == Blocks.ACACIA_FENCE_GATE || block == Blocks.DARK_OAK_FENCE_GATE ||
                    block == Blocks.MANGROVE_FENCE_GATE;
        }
        return false;
    }

    private void setGlassBreakingProgress(BlockPos pos, int progress) {
        if (this.zombie.getWorld() instanceof ServerWorld) {
            ((ServerWorld) this.zombie.getWorld()).getServer().getPlayerManager().sendToAll(
                    new BlockBreakingProgressS2CPacket(this.zombie.getId(), pos, progress)
            );
        }
    }

    private LivingEntity findTargetThroughGlass() {
        Box detectionBox = this.zombie.getBoundingBox().expand(24.0);
        List<LivingEntity> potentialTargets = this.zombie.getWorld().getEntitiesByClass(LivingEntity.class, detectionBox, entity -> entity instanceof PlayerEntity);

        for (LivingEntity potentialTarget : potentialTargets) {
            if (canSeeThroughGlassOrBreakable(potentialTarget)) { // Utilisation de la nouvelle méthode
                return potentialTarget;
            }
        }
        return null;
    }

    private boolean canSeeThroughGlassOrBreakable(LivingEntity target) {
        Vec3d startPos = this.zombie.getEyePos();
        Vec3d endPos = target.getEyePos();
        RaycastContext context = new RaycastContext(startPos, endPos, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, this.zombie);
        BlockPos hitPos = this.zombie.getWorld().raycast(context).getBlockPos();
        Block hitBlock = this.zombie.getWorld().getBlockState(hitPos).getBlock();
        return isGlassOrBreakableBlock(hitPos); // Vérifie également les fences et portes de fence
    }

    private boolean isPartOfPatrol() {
        return ((IPatrolData) this.zombie).isHordeEntityPatrolling();
    }
}