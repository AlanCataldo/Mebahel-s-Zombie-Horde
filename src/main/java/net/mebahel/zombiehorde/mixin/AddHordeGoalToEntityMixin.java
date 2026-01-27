package net.mebahel.zombiehorde.mixin;

import net.mebahel.zombiehorde.entity.ai.BreakGlassGoal;
import net.mebahel.zombiehorde.entity.ai.ModHordeGoal;
import net.mebahel.zombiehorde.config.HordeMemberModConfig;
import net.mebahel.zombiehorde.config.ZombieHordeModConfig;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class AddHordeGoalToEntityMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void addPatrolGoal(CallbackInfo ci) {
        MobEntity mobEntity = (MobEntity) (Object) this;

        // Vérifie si l’EntityType de cette entité est dans la liste de configuration
        EntityType<?> entityType = mobEntity.getType();
        String entityId = EntityType.getId(entityType).toString();

        if (isEntityInHordeConfig(entityId)) {
            // Utilise un accessor pour obtenir goalSelector et ajoute le ModHordeGoal
            GoalSelector goalSelector = ((MobEntityAccessor) mobEntity).getGoalSelector();
            if (ZombieHordeModConfig.hordeMemberBreakGlass && ZombieHordeModConfig.hordeMemberBreakFence)
                goalSelector.add(1, new BreakGlassGoal(mobEntity, true, true));
            else if (!ZombieHordeModConfig.hordeMemberBreakGlass && ZombieHordeModConfig.hordeMemberBreakFence)
                goalSelector.add(1, new BreakGlassGoal(mobEntity, false, true));
            else if (ZombieHordeModConfig.hordeMemberBreakGlass && !ZombieHordeModConfig.hordeMemberBreakFence)
                goalSelector.add(1, new BreakGlassGoal(mobEntity, true, false));
            goalSelector.add(4, new ModHordeGoal(mobEntity, 0.93, 0.93));
        }
    }

    @Unique
    private boolean isEntityInHordeConfig(String entityId) {
        // Parcours de chaque composition de horde dans la configuration
        for (HordeMemberModConfig.HordeComposition composition : HordeMemberModConfig.hordeCompositions) {
            // Parcours de chaque type de mob dans la composition
            for (HordeMemberModConfig.HordeMobType mobType : composition.mobTypes) {
                // Vérifie si l'identifiant de l'entité correspond
                if (mobType.id.equals(entityId)) {
                    return true;
                }
            }
        }
        // Retourne false si l'identifiant de l'entité n'est trouvé dans aucune composition
        return false;
    }
}