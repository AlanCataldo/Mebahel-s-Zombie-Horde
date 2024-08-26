package net.mebahel.zombiehorde.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBiomeTags;
import net.mebahel.zombiehorde.MebahelZombieHorde;
import net.mebahel.zombiehorde.entity.ModEntities;
import net.mebahel.zombiehorde.entity.custom.ZombieHordeEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ZombieHordeManager {
    private static final int CHECK_INTERVAL = 10 * 60 * ModConfig.patrolSpawnDelay;
    private static final int NETHER_CHECK_INTERVAL = 300; // 1 minute
    private static int patrolCheckCounter = 0;
    private static int netherCheckCounter = 0;

    // Utilisation d'une map pour stocker la difficulté par monde
    private static final Map<ServerWorld, Integer> worldDifficultyLevels = new HashMap<>();

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
                        ModDifficultyState.createType(),
                        "zombie_horde_difficulty"
                );

                // Charger le niveau de difficulté pour le monde actuel
                int difficultyLevel = difficultyState.getDifficultyLevel();
                worldDifficultyLevels.put(world, difficultyLevel);
                System.out.println("[Mebahel's Zombie Horde] Loaded difficulty level for world " + world.getRegistryKey().getValue() + ": " + difficultyLevel);

                ServerTickEvents.START_SERVER_TICK.register(serverTick -> {
                    List<ServerPlayerEntity> players = world.getPlayers();
                    
                    if (!players.isEmpty()) {
                        if (isNightTime(world) && !ModConfig.spawnInDaylight)
                            patrolCheckCounter++;
                        else if (ModConfig.spawnInDaylight) {
                            patrolCheckCounter++;
                        }

                        if (patrolCheckCounter >= CHECK_INTERVAL) {
                            patrolCheckCounter = 0;
                            checkAndSpawnPatrol(world);
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
                });
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
                    spawnPatrol(world, spawnPos, patrolId, randomPlayer);
                    System.out.println("[Mebahel's Zombie Horde] Zombie horde spawning for at : " + spawnPos);
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
            if (player.getAdvancementTracker().getProgress(world.getServer().getAdvancementLoader().get(Identifier.of("minecraft", "nether/root"))).isDone()) {
                int difficultyLevel = 2;
                worldDifficultyLevels.put(world, difficultyLevel);
                difficultyState.setDifficultyLevel(difficultyLevel); // Sauvegarde la nouvelle difficulté pour ce monde
                System.out.println("[Mebahel's Zombie Horde] Difficulty increased to 2 due to Nether visit by " + player.getName().getString());
                break;
            }
        }
    }

    private static boolean isNightTime(ServerWorld world) {
        return !world.getDimension().hasFixedTime() && !world.isDay();
    }

    private static void spawnPatrol(ServerWorld world, BlockPos pos, UUID patrolId, PlayerEntity player) {
        Random random = new Random();
        BlockPos groundPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos);
        BlockPos leaderPos = getOffsetPosition(groundPos, random);
        BlockPos distantTarget = setRandomPatrolTarget(world, groundPos);
        System.out.println("DISTANT TARGEEEEET :" + distantTarget);
        int difficultyLevel = worldDifficultyLevels.getOrDefault(world, 1);
        int numFollowers = 4 + (2 * difficultyLevel) + random.nextInt(1 + (2 * difficultyLevel));

        EntityType<? extends ZombieHordeEntity> entityType = world.getBiome(player.getBlockPos()).isIn(ConventionalBiomeTags.DESERT)
                ? ModEntities.HUSK_HORDE
                : ModEntities.ZOMBIE_HORDE;

        spawnPatrolLeader(world, entityType, leaderPos, distantTarget, random, patrolId);
        spawnPatrolMember(world, entityType, numFollowers, groundPos, distantTarget, random, patrolId);
    }

    private static void spawnPatrolLeader(ServerWorld world, EntityType<? extends ZombieHordeEntity> entityType,
                                          BlockPos groundPos, BlockPos distantTarget, Random random, UUID patrolId) {
        ZombieHordeEntity leader = new ZombieHordeEntity(entityType, world);
        leader.setPatrolId(patrolId.toString());
        BlockPos leaderPos = getOffsetPosition(groundPos, random);
        leaderPos = findSafeSpawnPosition(world, leaderPos);  // Rechercher une position de spawn sûre
        leader.setPosition(leaderPos.getX() + 0.5, leaderPos.getY(), leaderPos.getZ() + 0.5);
        leader.setPatrolLeader(true);
        leader.setPatrolTarget(distantTarget);
        leader.setWasInitiallyInPatrol(true);
        leader.initialize(world, world.getLocalDifficulty(groundPos), SpawnReason.NATURAL, null);
        world.spawnEntity(leader);
        System.out.println("[PATROL TARGET DU MEMBRE] " + leader.getPatrolTarget());
    }

    private static void spawnPatrolMember(ServerWorld world, EntityType<? extends ZombieHordeEntity> entityType, int entityNumber,
                                          BlockPos initialPos, BlockPos distantTarget, Random random, UUID patrolId) {
        BlockPos currentSpawnPos = initialPos;

        for (int i = 0; i < entityNumber; i++) {
            ZombieHordeEntity member = new ZombieHordeEntity(entityType, world);
            member.setPatrolId(patrolId.toString());
            BlockPos memberSpawnPos = getOffsetPosition(currentSpawnPos, random);
            memberSpawnPos = findSafeSpawnPosition(world, memberSpawnPos);  // Rechercher une position de spawn sûre
            member.setPosition(memberSpawnPos.getX() + 0.5, memberSpawnPos.getY(), memberSpawnPos.getZ() + 0.5);
            member.setPatrolTarget(distantTarget);
            member.setWasInitiallyInPatrol(true);
            member.initialize(world, world.getLocalDifficulty(memberSpawnPos), SpawnReason.NATURAL, null);
            world.spawnEntity(member);
            System.out.println("[PATROL TARGET DU MEMBRE] " + member.getPatrolTarget());
            currentSpawnPos = memberSpawnPos;
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

        // Vérifier si la hauteur est correcte (ex: > 0)
        if (finalTargetPos.getY() == -64) {
            finalTargetPos = setRandomPatrolTarget(world, pos);
        }

        return finalTargetPos;
    }

    private static BlockPos findTopSolidBlock(ServerWorld world, BlockPos pos) {
        return world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos);
    }
}
