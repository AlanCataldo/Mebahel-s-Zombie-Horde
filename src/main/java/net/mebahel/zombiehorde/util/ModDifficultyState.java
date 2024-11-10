package net.mebahel.zombiehorde.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;

public class ModDifficultyState extends PersistentState {
    private int difficultyLevel;

    public ModDifficultyState(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
        this.markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("difficultyLevel", difficultyLevel);
        return nbt;
    }

    public static ModDifficultyState fromNbt(NbtCompound nbt) {
        return new ModDifficultyState(nbt.getInt("difficultyLevel"));
    }
}
