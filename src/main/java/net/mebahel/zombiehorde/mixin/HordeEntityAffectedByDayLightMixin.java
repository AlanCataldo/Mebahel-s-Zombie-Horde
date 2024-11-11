package net.mebahel.zombiehorde.mixin;

import net.mebahel.zombiehorde.util.IPatrolData;
import net.mebahel.zombiehorde.util.ModConfig;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public abstract class HordeEntityAffectedByDayLightMixin {

    // Injecte la logique avant le retour de isAffectedByDaylight
    @Inject(method = "isAffectedByDaylight", at = @At("HEAD"), cancellable = true)
    private void modifyIsAffectedByDaylight(CallbackInfoReturnable<Boolean> cir) {
        // Vérifie si l'entité implémente IPatrolData et fait partie d'une patrouille
        if (this instanceof IPatrolData patrolData && patrolData.isHordeEntityPatrolling()) {
            // Si la configuration "spawnInDaylight" est true, retourne false
            if (ModConfig.spawnInDaylight) {
                cir.setReturnValue(false);
            }
        }
    }
}