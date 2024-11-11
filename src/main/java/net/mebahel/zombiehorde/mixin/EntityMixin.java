package net.mebahel.zombiehorde.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Entity.class)
public abstract class EntityMixin {
    // Utilisez @Shadow pour accéder au champ dataTracker
    @Shadow
    protected DataTracker dataTracker;
}
