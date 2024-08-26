package net.mebahel.zombiehorde.util;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.PersistentState;

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

    public static ModDifficultyState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        ModDifficultyState state = new ModDifficultyState();
        state.difficultyLevel = nbt.getInt("difficultyLevel");
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putInt("difficultyLevel", difficultyLevel);
        return nbt;
    }

    // Méthode pour créer un PersistentState.Type
    public static PersistentState.Type<ModDifficultyState> createType() {
        return new PersistentState.Type<>(
                ModDifficultyState::new, // Supplier pour créer un nouvel état par défaut
                ModDifficultyState::fromNbt, // BiFunction pour désérialiser depuis NBT
                DataFixTypes.LEVEL // Type de correction des données
        );
    }
}
