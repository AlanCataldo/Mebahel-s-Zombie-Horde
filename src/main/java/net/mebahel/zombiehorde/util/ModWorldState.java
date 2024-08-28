package net.mebahel.zombiehorde.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;

public class ModWorldState extends PersistentState {
    private boolean isEventRegistered = false;

    // Vérifie si l'événement est déjà enregistré
    public boolean isEventRegistered() {
        return isEventRegistered;
    }

    // Définit l'état d'enregistrement de l'événement
    public void setEventRegistered(boolean isEventRegistered) {
        this.isEventRegistered = isEventRegistered;
        markDirty(); // Marque l'état comme modifié pour qu'il soit sauvegardé
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putBoolean("eventRegistered", isEventRegistered);
        return nbt;
    }

    public static ModWorldState fromNbt(NbtCompound nbt) {
        ModWorldState state = new ModWorldState();
        state.isEventRegistered = nbt.getBoolean("eventRegistered");
        return state;
    }
}
