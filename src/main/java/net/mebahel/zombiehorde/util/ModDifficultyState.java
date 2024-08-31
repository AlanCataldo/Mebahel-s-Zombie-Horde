package net.mebahel.zombiehorde.util;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;
import java.util.function.Supplier;
import java.util.function.Function;

public class ModDifficultyState extends PersistentState {
    private int difficultyLevel;

    public ModDifficultyState() {
        this.difficultyLevel = 1; // Valeur par défaut
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
        this.markDirty(); // Marquer comme modifié pour être sauvegardé
    }

    // Méthode simplifiée fromNbt pour correspondre à la signature Function<NbtCompound, T>
    public static ModDifficultyState fromNbt(NbtCompound nbt) {
        ModDifficultyState state = new ModDifficultyState();
        state.difficultyLevel = nbt.getInt("difficultyLevel");
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("difficultyLevel", difficultyLevel);
        return nbt;
    }

    // Méthode pour créer un PersistentState.Type
    public static PersistentState.Type<ModDifficultyState> createType() {
        Supplier<ModDifficultyState> supplier = ModDifficultyState::new;
        Function<NbtCompound, ModDifficultyState> function = ModDifficultyState::fromNbt;

        return new PersistentState.Type<>(
                supplier,  // Supplier pour créer un nouvel état par défaut
                function,  // Function pour désérialiser depuis NBT
                DataFixTypes.LEVEL // Type de correction des données
        );
    }
}
