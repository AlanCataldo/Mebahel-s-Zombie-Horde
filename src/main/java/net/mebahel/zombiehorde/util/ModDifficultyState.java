package net.mebahel.zombiehorde.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.PersistentState;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

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
        this.markDirty(); // toujours utile pour forcer la sauvegarde
    }

    // ✅ nouvelle signature (MC 1.21+)
    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        nbt.putInt("difficultyLevel", difficultyLevel);
        return nbt;
    }

    public static ModDifficultyState fromNbt(NbtCompound nbt) {
        return new ModDifficultyState(nbt.getInt("difficultyLevel"));
    }

    public static final Type<ModDifficultyState> TYPE = new Type<>(
            () -> new ModDifficultyState(1),
            (nbt, lookup) -> ModDifficultyState.fromNbt(nbt),
            null
    );
}
