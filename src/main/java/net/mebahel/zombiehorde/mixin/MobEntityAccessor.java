package net.mebahel.zombiehorde.mixin;

import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MobEntity.class)
public interface MobEntityAccessor {

    @Accessor("goalSelector")
    GoalSelector getGoalSelector();

    // dans MobEntityAccessor.java
    @Accessor("persistent")
    void mebahel$setPersistentFlag(boolean value);

    @Accessor("persistent")
    boolean mebahel$getPersistentFlag();
}