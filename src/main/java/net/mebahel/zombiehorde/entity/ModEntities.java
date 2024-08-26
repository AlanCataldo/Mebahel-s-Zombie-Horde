package net.mebahel.zombiehorde.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.mebahel.zombiehorde.MebahelZombieHorde;
import net.mebahel.zombiehorde.entity.custom.ZombieHordeEntity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<ZombieHordeEntity> ZOMBIE_HORDE  = Registry.register(
            Registries.ENTITY_TYPE, Identifier.of(MebahelZombieHorde.MOD_ID, "zombie_horde"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, ZombieHordeEntity::new)
                    .dimensions(EntityDimensions.fixed(0.65f, 1.95f)).build());

    public static final EntityType<ZombieHordeEntity> HUSK_HORDE  = Registry.register(
            Registries.ENTITY_TYPE, Identifier.of(MebahelZombieHorde.MOD_ID, "husk_horde"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, ZombieHordeEntity::new)
                    .dimensions(EntityDimensions.fixed(0.65f, 1.95f)).build());
}
