package net.mebahel.zombiehorde.entity.ai;

import net.mebahel.zombiehorde.entity.custom.ZombieHordeEntity;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.EnumSet;

public class BreakGlassOnPathGoal extends Goal {
    private final ZombieHordeEntity zombie;
    private Path currentPath;
    private int pathIndex;

    public BreakGlassOnPathGoal(ZombieHordeEntity zombie) {
        this.zombie = zombie;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        currentPath = zombie.getNavigation().getCurrentPath();
        return currentPath != null && !currentPath.isFinished();
    }

    @Override
    public void start() {
        pathIndex = 0;
    }

    @Override
    public void tick() {
        if (currentPath != null && pathIndex < currentPath.getLength()) {
            PathNode node = currentPath.getNode(pathIndex);
            BlockPos nodePos = new BlockPos(node.x, node.y, node.z);
            World world = zombie.getWorld();

            Block block = world.getBlockState(nodePos).getBlock();
            if (block == Blocks.GLASS || block == Blocks.GLASS_PANE) {
                world.breakBlock(nodePos, true); // Casser la vitre
            }

            pathIndex++;
        }
    }

    @Override
    public boolean shouldContinue() {
        return currentPath != null && pathIndex < currentPath.getLength();
    }
}
