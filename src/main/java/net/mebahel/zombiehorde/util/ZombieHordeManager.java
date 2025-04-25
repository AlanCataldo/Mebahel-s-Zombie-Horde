package net.mebahel.zombiehorde.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.mebahel.zombiehorde.MebahelZombieHorde;
import net.mebahel.zombiehorde.entity.ModEntities;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.*;

public class ZombieHordeManager {
    private static final int CHECK_INTERVAL = 20 * 60 * ModConfig.patrolSpawnDelay;
    private static final int NETHER_CHECK_INTERVAL = 300;
    private static int patrolCheckCounter = 0;
    private static int netherCheckCounter = 0;
    private static final Map<ServerWorld, Integer> worldDifficultyLevels = new HashMap<>();
    private static final Map<ServerWorld, ServerTickEvents.EndTick> registeredListeners = new HashMap<>();

    public static void register() {
        if (!ModConfig.patrolSpawning) {
            MebahelZombieHorde.LOGGER.info("[Mebahel's Zombie Horde] Zombie horde spawning is disabled in config file.");
            return;
        }

        MebahelZombieHorde.LOGGER.info("[Mebahel's Zombie Horde] Registering zombie horde spawning for " + MebahelZombieHorde.MOD_ID + ".");

        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.getRegistryKey() == World.OVERWORLD) {
                PersistentStateManager stateManager = world.getPersistentStateManager();
                ModDifficultyState difficultyState = stateManager.getOrCreate(
                        ModDifficultyState::fromNbt,
                        () -> new ModDifficultyState(1),
                        "zombie_horde_difficulty"
                );

                int difficultyLevel = difficultyState.getDifficultyLevel();
                worldDifficultyLevels.put(world, difficultyLevel);
                System.out.println("[Mebahel's Zombie Horde] Loaded difficulty level for world " + world.getRegistryKey().getValue() + ": " + difficultyLevel);

                ServerTickEvents.EndTick listener = serverTick -> {
                    if (serverTick.getWorld(World.OVERWORLD) == world) {
                        if (isNightTime(world) && !ModConfig.spawnInDaylight) {
                            patrolCheckCounter++;
                        } else if (ModConfig.spawnInDaylight) {
                            patrolCheckCounter++;
                        }

                        if (patrolCheckCounter >= CHECK_INTERVAL) {
                            for (int i = 0; i < ModConfig.hordeNumber; i++) {
                                Random random = new Random();
                                float randomValue = random.nextFloat();
                                float spawnChance = ModConfig.hordeSpawnChance;

                                patrolCheckCounter = 0;
                                if (randomValue <= spawnChance)
                                    checkAndSpawnPatrol(world);
                                else
                                    System.out.println("[Mebahel's Zombie Horde] You are lucky... The Horde didn't spawn this time...");
                            }
                        }

                        if (ModConfig.enableDifficultySystem) {
                            if (worldDifficultyLevels.get(world) == 1) {
                                netherCheckCounter++;
                                if (netherCheckCounter >= NETHER_CHECK_INTERVAL) {
                                    netherCheckCounter = 0;
                                    checkNetherVisit(world, difficultyState);
                                }
                            }
                        } else {
                            worldDifficultyLevels.put(world, 1);
                        }
                    }
                };

                ServerTickEvents.END_SERVER_TICK.register(listener);
                registeredListeners.put(world, listener);
            }
        });

        ServerWorldEvents.UNLOAD.register((server, world) -> {
            if (world.getRegistryKey() == World.OVERWORLD) {
                registeredListeners.remove(world);
                MebahelZombieHorde.LOGGER.info("[Mebahel's Zombie Horde] World unloaded, event listener removed.");
            }
        });
    }

    private static void checkAndSpawnPatrol(ServerWorld world) {
        List<ServerPlayerEntity> players = world.getPlayers();

        if (!players.isEmpty()) {
            Random random = new Random();
            PlayerEntity randomPlayer = players.get(random.nextInt(players.size()));

            if (randomPlayer.isAlive()) {
                BlockPos spawnPos = findSpawnPosition(world, randomPlayer);
                if (spawnPos != null) {
                    UUID patrolId = UUID.randomUUID();
                    spawnPatrol(world, spawnPos, patrolId);
                }
            }
        }
    }

    private static BlockPos findSpawnPosition(ServerWorld world, PlayerEntity player) {
        int x = 30 + world.random.nextInt(30);
        int z = 30 + world.random.nextInt(30);

        if (world.random.nextBoolean())
            x = -x;
        if (world.random.nextBoolean())
            z = -z;

        return player.getBlockPos().add(x, 0, z);
    }

    private static void checkNetherVisit(ServerWorld world, ModDifficultyState difficultyState) {
        List<ServerPlayerEntity> players = world.getPlayers();

        for (ServerPlayerEntity player : players) {
            if (player.getAdvancementTracker().getProgress(world.getServer().getAdvancementLoader().get(new Identifier("minecraft", "nether/root"))).isDone()) {
                int difficultyLevel = 2;
                worldDifficultyLevels.put(world, difficultyLevel);
                difficultyState.setDifficultyLevel(difficultyLevel);
                System.out.println("[Mebahel's Zombie Horde] Difficulty increased to 2 due to Nether visit by " + player.getName().getString());
                break;
            }
        }
    }

    private static boolean isNightTime(ServerWorld world) {
        return !world.getDimension().hasFixedTime() && !world.isDay();
    }

    private static HordeMemberModConfig.HordeComposition getRandomHordeComposition(Random random) {
        List<HordeMemberModConfig.HordeComposition> compositions = HordeMemberModConfig.hordeCompositions;
        int totalWeight = compositions.stream().mapToInt(c -> c.weight).sum();

        int randomValue = random.nextInt(totalWeight);
        for (HordeMemberModConfig.HordeComposition composition : compositions) {
            randomValue -= composition.weight;
            if (randomValue < 0) {
                return composition;
            }
        }

        return compositions.get(0);
    }


    private static EntityType<?> getRandomEntityTypeFromComposition(Random random, HordeMemberModConfig.HordeComposition composition) {
        int totalWeight = composition.mobTypes.stream().mapToInt(mobType -> mobType.weight).sum();
        int randomValue = random.nextInt(totalWeight);

        for (HordeMemberModConfig.HordeMobType mobType : composition.mobTypes) {
            randomValue -= mobType.weight;
            if (randomValue < 0) {
                Identifier entityId = new Identifier(mobType.id);
                return Registries.ENTITY_TYPE.getOrEmpty(entityId).orElse(null);
            }
        }

        return null;
    }

    private static void spawnPatrol(ServerWorld world, BlockPos pos, UUID patrolId) {
        Random random = new Random();
        BlockPos groundPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos);
        BlockPos leaderPos = getOffsetPosition(groundPos, random);
        BlockPos distantTarget = setRandomPatrolTarget(world, groundPos);
        int difficultyLevel = worldDifficultyLevels.getOrDefault(world, 1);
        int numFollowers = 4 + (2 * difficultyLevel) + random.nextInt(1 + (2 * difficultyLevel));

        if (ModConfig.randomNumberHordeReinforcements > 0) {
            int reinforcement = random.nextInt(ModConfig.randomNumberHordeReinforcements);
            numFollowers += reinforcement;
            System.out.printf("[Mebahel's Zombie Horde] The Horde has been reinforced by %d Zombies.%n", reinforcement);
        }
        HordeMemberModConfig.HordeComposition composition = getRandomHordeComposition(random);
        if (composition == null) {
            System.err.println("No valid horde composition found.");
            return;
        }
        spawnPatrolLeader(composition, world, leaderPos, distantTarget, random, patrolId, difficultyLevel);
        spawnPatrolMember(composition, world, numFollowers, groundPos, distantTarget, random, patrolId, difficultyLevel);
        System.out.println("[Mebahel's Zombie Horde]" + leaderPos.getX() + ", " + leaderPos.getY() + ", " + leaderPos.getZ() + ".");
        String message = "A horde has spawned near " + leaderPos.getX() + ", " + leaderPos.getY() + ", " + leaderPos.getZ() + ".";
        if (ModConfig.showHordeSpawningMessage) {
            world.getServer().getPlayerManager().getPlayerList().forEach(player ->
                    player.sendMessage(Text.literal(message), false)
            );
        }
    }

    private static void spawnPatrolLeader(HordeMemberModConfig.HordeComposition composition, ServerWorld world, BlockPos groundPos, BlockPos distantTarget, Random random,
                                          UUID patrolId, int difficultyLevel) {
        EntityType<?> randomEntityType = getRandomEntityTypeFromComposition(random, composition);
        if (randomEntityType == null) {
            System.err.println("[Mebahel's Zombie Horde] Error: Invalid or unregistered entity type in horde composition.");
            return; // Return early to prevent the crash
        }

        var leader = randomEntityType.create(world);
        if (leader == null) {
            System.err.println("[Mebahel's Zombie Horde] Error: Failed to create entity of type " + randomEntityType);
            return; // Return early if the entity could not be created
        }

        BlockPos leaderPos = findSafeSpawnPosition(world, groundPos);
        leader.setPosition(leaderPos.getX() + 0.5, leaderPos.getY(), leaderPos.getZ() + 0.5);

        if (leader instanceof MobEntity livingMember) {
            livingMember.setPersistent();

            // Additional configuration, such as health bonuses
            if (ModConfig.hordeMemberBonusHealth > 0) {
                livingMember.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)
                        .setBaseValue(livingMember.getMaxHealth() + ModConfig.hordeMemberBonusHealth);
                livingMember.setHealth(livingMember.getMaxHealth());
            }
        }

        IPatrolData patrolData = (IPatrolData) leader;
        patrolData.setHordeEntityPatrolLeader(true);
        patrolData.setHordeEntityPatrolTarget(distantTarget);
        patrolData.setHordeEntityPatrolling(true);
        patrolData.setHordeEntityPatrolId(patrolId.toString());

        equipWithGear(leader, random, composition, difficultyLevel);
        world.spawnEntity(leader);
    }

    private static void spawnPatrolMember(HordeMemberModConfig.HordeComposition composition, ServerWorld world, int entityNumber,
                                          BlockPos initialPos, BlockPos distantTarget, Random random, UUID patrolId, int difficultyLevel) {
        BlockPos currentSpawnPos = initialPos;

        for (int i = 0; i < entityNumber; i++) {
            EntityType<?> randomEntityType = getRandomEntityTypeFromComposition(random, composition);
            if (randomEntityType == null) {
                randomEntityType = ModEntities.ZOMBIE_HORDE;
                System.err.println("[Mebahel's Zombie Horde] Error: Invalid or unregistered entity type in horde composition.");
                continue; // Skip this iteration if the entity type is invalid
            }

            var member = randomEntityType.create(world);
            if (member == null) {
                System.err.println("[Mebahel's Zombie Horde] Error: Failed to create entity of type " + randomEntityType);
                continue; // Skip this iteration if the entity could not be created
            }

            BlockPos memberSpawnPos = findSafeSpawnPosition(world, currentSpawnPos);
            member.setPosition(memberSpawnPos.getX() + 0.5, memberSpawnPos.getY(), memberSpawnPos.getZ() + 0.5);

            if (member instanceof MobEntity livingMember) {
                livingMember.setPersistent();

                // Additional configuration, such as health bonuses
                if (ModConfig.hordeMemberBonusHealth > 0) {
                    livingMember.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)
                            .setBaseValue(livingMember.getMaxHealth() + ModConfig.hordeMemberBonusHealth);
                    livingMember.setHealth(livingMember.getMaxHealth());
                }
            }

            IPatrolData patrolData = (IPatrolData) member;
            patrolData.setHordeEntityPatrolTarget(distantTarget);
            patrolData.setHordeEntityPatrolling(true);
            patrolData.setHordeEntityPatrolId(patrolId.toString());

            equipWithGear(member, random, composition, difficultyLevel);
            world.spawnEntity(member);
            currentSpawnPos = getOffsetPosition(currentSpawnPos, random);
        }
    }

    private static BlockPos findSafeSpawnPosition(ServerWorld world, BlockPos pos) {
        BlockPos safePos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos);
        return safePos;
    }

    private static BlockPos getOffsetPosition(BlockPos basePos, Random random) {
        int offsetX = 5 + random.nextInt(5);
        int offsetZ = 5 + random.nextInt(5);

        if (random.nextBoolean()) offsetX = -offsetX;
        if (random.nextBoolean()) offsetZ = -offsetZ;

        return basePos.add(offsetX, 0, offsetZ);
    }

    public static BlockPos setRandomPatrolTarget(ServerWorld world, BlockPos pos) {
        int x = 150 + world.random.nextInt(150);
        int z = 150 + world.random.nextInt(150);

        if (world.random.nextBoolean()) x = -x;
        if (world.random.nextBoolean()) z = -z;

        BlockPos roughTargetPos = new BlockPos(pos.getX() + x, world.getHeight(), pos.getZ() + z);

        BlockPos finalTargetPos = findTopSolidBlock(world, roughTargetPos);

        if (finalTargetPos.getY() == -64) {
            finalTargetPos = setRandomPatrolTarget(world, pos);
        }

        return finalTargetPos;
    }

    private static BlockPos findTopSolidBlock(ServerWorld world, BlockPos pos) {
        return world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos);
    }
    private static HordeMemberModConfig.HordeMobType getMobTypeFromComposition(Entity entity, HordeMemberModConfig.HordeComposition composition) {
        String mobId = Registries.ENTITY_TYPE.getId(entity.getType()).toString();

        for (HordeMemberModConfig.HordeMobType mobType : composition.mobTypes) {
            if (mobType.id.equals(mobId)) {
                return mobType;
            }
        }

        return null; // Aucune configuration spécifique trouvée pour ce type de mob
    }

    private static void equipWithGear(Entity entity, Random random, HordeMemberModConfig.HordeComposition composition, int difficultyLevel) {
        float armorChance = 0.06f * difficultyLevel;
        float weaponChance = 0.13f * difficultyLevel;

        // Équipement d'armure basé sur les probabilités
        if (random.nextFloat() < armorChance) {
            entity.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        }
        if (random.nextFloat() < armorChance) {
            entity.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        }
        if (random.nextFloat() < armorChance) {
            entity.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        }
        if (random.nextFloat() < armorChance) {
            entity.equipStack(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        }

        // Vérification pour les PILLAGER et VINDICATOR (skip weaponChance)
        if (entity.getType() == EntityType.PILLAGER || entity.getType() == EntityType.VINDICATOR) {
            // Sélection d'une arme depuis la configuration
            HordeMemberModConfig.HordeMobType mobType = getMobTypeFromComposition(entity, composition);
            if (mobType != null && mobType.weapons != null && !mobType.weapons.isEmpty()) {
                double totalChance = mobType.weapons.stream().mapToDouble(w -> w.chance).sum();
                double randomChance = random.nextFloat() * totalChance;

                for (HordeMemberModConfig.WeaponConfig weapon : mobType.weapons) {
                    randomChance -= weapon.chance;
                    if (randomChance <= 0) {
                        ItemStack weaponStack = new ItemStack(Registries.ITEM.get(new Identifier(weapon.itemId)));
                        entity.equipStack(EquipmentSlot.MAINHAND, weaponStack);
                        return; // Arme trouvée et équipée
                    }
                }
            }

            // Arme par défaut si aucune arme configurée pour ce mob
            if (entity.getType() == EntityType.PILLAGER) {
                entity.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            } else if (entity.getType() == EntityType.VINDICATOR) {
                entity.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            }
            return; // Skip la logique suivante
        }

        // Vérification générale avec weaponChance pour les autres types d'entités
        if (random.nextFloat() < weaponChance) {
            // Sélection de l'arme pour les autres types de mobs
            HordeMemberModConfig.HordeMobType mobType = getMobTypeFromComposition(entity, composition);
            if (mobType != null && mobType.weapons != null && !mobType.weapons.isEmpty()) {
                double totalChance = mobType.weapons.stream().mapToDouble(w -> w.chance).sum();
                double randomChance = random.nextFloat() * totalChance;

                for (HordeMemberModConfig.WeaponConfig weapon : mobType.weapons) {
                    randomChance -= weapon.chance;
                    if (randomChance <= 0) {
                        ItemStack weaponStack = new ItemStack(Registries.ITEM.get(new Identifier(weapon.itemId)));
                        entity.equipStack(EquipmentSlot.MAINHAND, weaponStack);
                        return; // Arme trouvée et équipée
                    }
                }
            }
        }
    }
}
