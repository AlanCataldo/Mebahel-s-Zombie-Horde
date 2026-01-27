package net.mebahel.zombiehorde.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.mebahel.zombiehorde.MebahelZombieHorde;
import net.mebahel.zombiehorde.config.HordeMemberModConfig;
import net.mebahel.zombiehorde.config.ZombieHordeModConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.*;

public class ZombieHordeManager {
    private static final Map<ServerWorld, Long> lastHordeSpawnTime = new HashMap<>();
    private static final Map<ServerWorld, Long> nextScheduledHordeTick = new HashMap<>();
    private static final int NETHER_CHECK_INTERVAL = 300;
    private static int netherCheckCounter = 0;
    private static final Map<ServerWorld, Integer> worldDifficultyLevels = new HashMap<>();
    private static final Map<ServerWorld, ServerTickEvents.EndTick> registeredListeners = new HashMap<>();
    private static final int TEMP_PERSIST_TICKS = 20 * 90; // 1m30 = 1800 ticks

    public static void register() {
        if (!ZombieHordeModConfig.patrolSpawning) {
            MebahelZombieHorde.LOGGER.info("[Mebahel's Zombie Horde] Zombie horde spawning is disabled in config file.");
            return;
        }

        MebahelZombieHorde.LOGGER.info("[Mebahel's Zombie Horde] Registering zombie horde spawning for " + MebahelZombieHorde.MOD_ID + ".");

        ServerWorldEvents.LOAD.register((server, world) -> {
            PersistentStateManager stateManager = world.getPersistentStateManager();
            ModDifficultyState difficultyState = stateManager.getOrCreate(
                    ModDifficultyState.TYPE,
                    "zombie_horde_difficulty"
            );

            int difficultyLevel = difficultyState.getDifficultyLevel();
            worldDifficultyLevels.put(world, difficultyLevel);
            lastHordeSpawnTime.put(world, world.getTime());

            MebahelZombieHorde.LOGGER.info("[Zombie Horde] Loaded difficulty for world " + world.getRegistryKey().getValue());

            ServerTickEvents.EndTick listener = serverTick -> {
                long currentTick = world.getTime();

                ZombieHordeModConfig.HordeDelayResult delayInfo = ZombieHordeModConfig.getHordeDelayInTicksWithUnit();
                long delayInTicks = delayInfo.ticks;

                if (!nextScheduledHordeTick.containsKey(world)) {
                    long targetTick;

                    if (delayInfo.unit.equals("day")) {
                        long nextFullDay = ((currentTick / 24000L) + 1) * 24000L;
                        long randomOffset = world.random.nextInt(24000);
                        targetTick = nextFullDay + randomOffset;
                    } else {
                        targetTick = currentTick + delayInTicks;
                    }

                    nextScheduledHordeTick.put(world, targetTick);
                    System.out.println("[Zombie Horde] Next horde scheduled for: " + targetTick + " in " + world.getRegistryKey().getValue());
                }

                long scheduledTick = nextScheduledHordeTick.getOrDefault(world, Long.MAX_VALUE);
                boolean canSpawnNow = ZombieHordeModConfig.spawnInDaylight || isNightTime(world);

                if (canSpawnNow && currentTick >= scheduledTick) {
                    nextScheduledHordeTick.remove(world);

                    for (int i = 0; i < ZombieHordeModConfig.hordeNumber; i++) {
                        if (world.random.nextFloat() <= ZombieHordeModConfig.hordeSpawnChance) {
                            checkAndSpawnPatrol(world);
                        } else {
                            System.out.println("[Zombie Horde] Horde didn't spawn this time (chance miss).");
                        }
                    }
                }

                if (ZombieHordeModConfig.enableDifficultySystem && world.getRegistryKey() == World.OVERWORLD) {
                    if (worldDifficultyLevels.get(world) == 1) {
                        netherCheckCounter++;
                        if (netherCheckCounter >= NETHER_CHECK_INTERVAL) {
                            netherCheckCounter = 0;
                            checkNetherVisit(world, difficultyState);
                        }
                    }
                }
            };

            ServerTickEvents.END_SERVER_TICK.register(listener);
            registeredListeners.put(world, listener);
        });

        ServerWorldEvents.UNLOAD.register((server, world) -> {
            if (world.getRegistryKey() == World.OVERWORLD) {
                registeredListeners.remove(world);
                lastHordeSpawnTime.remove(world);
                MebahelZombieHorde.LOGGER.info("[Mebahel's Zombie Horde] World unloaded, event listener removed.");
            }
        });
    }

    private static void markTemporaryPersistent(ServerWorld world, Entity entity) {
        if (!(entity instanceof MobEntity mob)) return;


        mob.setPersistent(); // met persistent = true


        long expireAt = world.getTime() + TEMP_PERSIST_TICKS;
        ((TemporaryPersistentMob) mob).mebahel$setPersistentUntil(expireAt);
    }

    // -------------------- API --------------------
    public static void forceSpawnPatrol(ServerWorld world, BlockPos pos, UUID patrolId) {
        spawnPatrol(world, pos, patrolId);
    }

    public static Long getNextScheduledHordeTick(ServerWorld world) {
        return nextScheduledHordeTick.get(world);
    }

    // -------------------- SPAWN LOGIC --------------------
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
        int x = ZombieHordeModConfig.hordeSpawnDistanceFromPlayer + world.random.nextInt(20);
        int z = ZombieHordeModConfig.hordeSpawnDistanceFromPlayer + world.random.nextInt(20);

        if (world.random.nextBoolean()) x = -x;
        if (world.random.nextBoolean()) z = -z;

        BlockPos base = player.getBlockPos().add(x, 0, z);
        return new BlockPos(base.getX(), player.getBlockPos().getY(), base.getZ()); // ✅ on conserve Y du joueur
    }

    private static void checkNetherVisit(ServerWorld world, ModDifficultyState difficultyState) {
        List<ServerPlayerEntity> players = world.getPlayers();

        for (ServerPlayerEntity player : players) {
            if (player.getAdvancementTracker().getProgress(world.getServer().getAdvancementLoader().get(Identifier.of("minecraft", "nether/root"))).isDone()) {
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

    private static HordeMemberModConfig.HordeComposition getRandomHordeComposition(Random random, ServerWorld world) {
        String currentDimension = world.getRegistryKey().getValue().toString();

        List<HordeMemberModConfig.HordeComposition> eligible = HordeMemberModConfig.hordeCompositions.stream()
                .filter(comp -> comp.dimensions.contains(currentDimension))
                .toList();

        if (eligible.isEmpty()) {
            System.err.println("[Mebahel's Zombie Horde] No eligible horde compositions for dimension: " + currentDimension);
            return null;
        }

        int totalWeight = eligible.stream().mapToInt(c -> c.weight).sum();
        int randomValue = random.nextInt(totalWeight);

        for (HordeMemberModConfig.HordeComposition comp : eligible) {
            randomValue -= comp.weight;
            if (randomValue < 0) {
                return comp;
            }
        }

        return eligible.get(0);
    }

    private static EntityType<?> getRandomEntityTypeFromComposition(Random random, HordeMemberModConfig.HordeComposition composition) {
        int totalWeight = composition.mobTypes.stream().mapToInt(mobType -> mobType.weight).sum();
        int randomValue = random.nextInt(totalWeight);

        for (HordeMemberModConfig.HordeMobType mobType : composition.mobTypes) {
            randomValue -= mobType.weight;
            if (randomValue < 0) {
                Identifier entityId = Identifier.of(mobType.id);
                return Registries.ENTITY_TYPE.getOrEmpty(entityId).orElse(null);
            }
        }

        return null;
    }

    private static void spawnPatrol(ServerWorld world, BlockPos pos, UUID patrolId) {
        Random random = new Random();
        BlockPos playerGroundPos = findSafeSpawnPosition(world, pos);
        BlockPos distantTarget = setRandomPatrolTarget(world, playerGroundPos);
        int difficultyLevel = worldDifficultyLevels.getOrDefault(world, 1);
        int numFollowers = 4 + (2 * difficultyLevel) + random.nextInt(1 + (2 * difficultyLevel));

        if (ZombieHordeModConfig.randomNumberHordeReinforcements > 0) {
            int reinforcement = random.nextInt(ZombieHordeModConfig.randomNumberHordeReinforcements);
            numFollowers += reinforcement;
            System.out.printf("[Mebahel's Zombie Horde] The Horde has been reinforced by %d Zombies.%n", reinforcement);
        }

        HordeMemberModConfig.HordeComposition composition = getRandomHordeComposition(random, world);
        if (composition == null) {
            System.err.println("[Mebahel's Zombie Horde] No valid horde composition found for this dimension. Skipping spawn.");
            return;
        }

        List<BlockPos> validPositions = findValidSpawnPositionsAround(world, playerGroundPos, 8);
        if (validPositions.isEmpty()) {
            System.err.println("[Mebahel's Zombie Horde] ❌ No valid spawn positions found near: " + playerGroundPos);
            return;
        }

        BlockPos leaderPos = validPositions.remove(random.nextInt(validPositions.size()));
        spawnPatrolLeader(composition, world, leaderPos, distantTarget, random, patrolId, difficultyLevel);

        int remaining = Math.min(numFollowers, validPositions.size());
        for (int i = 0; i < remaining; i++) {
            BlockPos spawnPos = validPositions.remove(random.nextInt(validPositions.size()));
            spawnPatrolMember(composition, world, 1, spawnPos, distantTarget, random, patrolId, difficultyLevel);
        }

        if (ZombieHordeModConfig.showHordeSpawningMessage) {
            String coords = leaderPos.getX() + " " + leaderPos.getY() + " " + leaderPos.getZ();
            Text clickableCoords = Text.literal("[" + coords + "]")
                    .setStyle(Style.EMPTY
                            .withColor(Formatting.AQUA)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp @s " + coords))
                            .withHoverEvent(new net.minecraft.text.HoverEvent(
                                    net.minecraft.text.HoverEvent.Action.SHOW_TEXT,
                                    Text.literal("Click to teleport")
                            ))
                    );

            Text finalMsg = Text.literal("A horde has spawned at : ")
                    .append(clickableCoords)
                    .append("!");

            for (ServerPlayerEntity p : world.getPlayers()) {
                p.sendMessage(finalMsg, false);
            }
        }
    }

    private static List<BlockPos> findValidSpawnPositionsAround(ServerWorld world, BlockPos center, int radius) {
        List<BlockPos> validPositions = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius) continue;

                BlockPos candidate = center.add(dx, 0, dz);
                BlockPos below = candidate.down();
                BlockPos above = candidate.up();

                boolean solidBelow = world.getBlockState(below).isSolidBlock(world, below);
                boolean airHere = world.getBlockState(candidate).isAir();
                boolean airAbove = world.getBlockState(above).isAir();

                boolean isLiquidHere = world.getBlockState(candidate).getFluidState().isStill();
                boolean isLiquidBelow = world.getBlockState(below).getFluidState().isStill();

                if (solidBelow && airHere && airAbove && !isLiquidHere && !isLiquidBelow) {
                    validPositions.add(candidate);
                }
            }
        }

        return validPositions;
    }

    private static void spawnPatrolLeader(HordeMemberModConfig.HordeComposition composition, ServerWorld world, BlockPos groundPos,
                                          BlockPos distantTarget, Random random, UUID patrolId, int difficultyLevel) {
        EntityType<?> randomEntityType = getRandomEntityTypeFromComposition(random, composition);
        if (randomEntityType == null) {
            System.err.println("[Mebahel's Zombie Horde] Error: Invalid or unregistered entity type in horde composition.");
            return;
        }

        var leader = randomEntityType.create(world);
        if (leader == null) {
            System.err.println("[Mebahel's Zombie Horde] Error: Failed to create entity of type " + randomEntityType);
            return;
        }

        BlockPos leaderPos = findSafeSpawnPosition(world, groundPos);
        leader.setPosition(leaderPos.getX() + 0.5, leaderPos.getY(), leaderPos.getZ() + 0.5);

        if (leader instanceof MobEntity livingMember) {
            if (ZombieHordeModConfig.hordeMemberBonusHealth > 0) {
                livingMember.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)
                        .setBaseValue(livingMember.getMaxHealth() + ZombieHordeModConfig.hordeMemberBonusHealth);
                livingMember.setHealth(livingMember.getMaxHealth());
            }
        }

        IPatrolData patrolData = (IPatrolData) leader;
        patrolData.setHordeEntityPatrolLeader(true);
        patrolData.setHordeEntityPatrolTarget(distantTarget);
        patrolData.setHordeEntityPatrolling(true);
        patrolData.setHordeEntityPatrolId(patrolId.toString());

        equipWithGear(leader, random, composition, difficultyLevel);

        // ✅ persistent 1m30
        markTemporaryPersistent(world, leader);

        // (optionnel) force chunk load au moment du spawn
        world.getChunk(leaderPos);

        world.spawnEntity(leader);
    }

    private static void spawnPatrolMember(HordeMemberModConfig.HordeComposition composition, ServerWorld world, int entityNumber,
                                          BlockPos initialPos, BlockPos distantTarget, Random random, UUID patrolId, int difficultyLevel) {
        BlockPos currentSpawnPos = initialPos;

        for (int i = 0; i < entityNumber; i++) {
            EntityType<?> randomEntityType = getRandomEntityTypeFromComposition(random, composition);
            if (randomEntityType == null) {
                randomEntityType = EntityType.ZOMBIE;
                System.err.println("[Mebahel's Zombie Horde] Error: Invalid or unregistered entity type in horde composition.");
                continue;
            }

            var member = randomEntityType.create(world);
            if (member == null) {
                System.err.println("[Mebahel's Zombie Horde] Error: Failed to create entity of type " + randomEntityType);
                continue;
            }

            BlockPos memberSpawnPos = findSafeSpawnPosition(world, currentSpawnPos);
            member.setPosition(memberSpawnPos.getX() + 0.5, memberSpawnPos.getY(), memberSpawnPos.getZ() + 0.5);

            if (member instanceof MobEntity livingMember) {
                if (ZombieHordeModConfig.hordeMemberBonusHealth > 0) {
                    livingMember.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)
                            .setBaseValue(livingMember.getMaxHealth() + ZombieHordeModConfig.hordeMemberBonusHealth);
                    livingMember.setHealth(livingMember.getMaxHealth());
                }
            }

            IPatrolData patrolData = (IPatrolData) member;
            patrolData.setHordeEntityPatrolTarget(distantTarget);
            patrolData.setHordeEntityPatrolling(true);
            patrolData.setHordeEntityPatrolId(patrolId.toString());

            equipWithGear(member, random, composition, difficultyLevel);

            // ✅ persistent 1m30
            markTemporaryPersistent(world, member);

            // (optionnel) force chunk load au moment du spawn
            world.getChunk(memberSpawnPos);

            world.spawnEntity(member);
            currentSpawnPos = getOffsetPosition(currentSpawnPos, random);
        }
    }

    private static BlockPos findSafeSpawnPosition(ServerWorld world, BlockPos pos) {
        boolean isCeilingWorld = world.getDimension().hasCeiling();
        int playerY = pos.getY();
        int maxAllowedY = isCeilingWorld ? 122 : world.getTopY() - 1;

        int startY = Math.min(playerY, maxAllowedY);
        int minY = world.getBottomY();

        System.out.println("[Zombie Horde][DEBUG] Finding safe spawn position in world: " + world.getRegistryKey().getValue());
        System.out.println("[Zombie Horde][DEBUG] hasCeiling = " + isCeilingWorld + ", startY = " + startY + ", maxY = " + maxAllowedY + ", minY = " + minY);

        // Étape 1 : montée limitée
        for (int y = startY; y <= maxAllowedY; y++) {
            BlockPos check = new BlockPos(pos.getX(), y, pos.getZ());
            boolean solidBelow = !world.getBlockState(check).isAir();
            boolean airAbove = world.getBlockState(check.up()).isAir();

            if (solidBelow && airAbove) {
                System.out.printf("[Zombie Horde][DEBUG] ✅ Valid upward spawn at Y=%d%n", y + 1);
                return check.up();
            }
        }

        // Étape 2 : descente
        for (int y = startY - 1; y >= minY; y--) {
            BlockPos check = new BlockPos(pos.getX(), y, pos.getZ());
            boolean solidBelow = !world.getBlockState(check).isAir();
            boolean airAbove = world.getBlockState(check.up()).isAir();

            if (solidBelow && airAbove) {
                System.out.printf("[Zombie Horde][DEBUG] ✅ Valid downward spawn at Y=%d%n", y + 1);
                return check.up();
            }
        }

        System.err.println("[Zombie Horde][WARN] ❌ No valid spawn position found. Using fallback: " + pos);
        return pos;
    }

    private static BlockPos getOffsetPosition(BlockPos basePos, Random random) {
        int offsetX = 5 + random.nextInt(5);
        int offsetZ = 5 + random.nextInt(5);

        if (random.nextBoolean()) offsetX = -offsetX;
        if (random.nextBoolean()) offsetZ = -offsetZ;

        return basePos.add(offsetX, 0, offsetZ);
    }

    public static BlockPos setRandomPatrolTarget(ServerWorld world, BlockPos pos) {
        return setRandomPatrolTarget(world, pos, 0);
    }

    private static BlockPos setRandomPatrolTarget(ServerWorld world, BlockPos pos, int tries) {
        if (tries >= 10) {
            System.err.println("[Mebahel's Zombie Horde] Failed to find valid patrol target after 10 attempts.");
            return pos;
        }

        int x = 150 + world.random.nextInt(110);
        int z = 150 + world.random.nextInt(110);

        if (world.random.nextBoolean()) x = -x;
        if (world.random.nextBoolean()) z = -z;

        BlockPos roughTargetPos = new BlockPos(pos.getX() + x, world.getHeight(), pos.getZ() + z);
        BlockPos finalTargetPos = findTopSolidBlock(world, roughTargetPos);

        if (finalTargetPos.getY() == -64) {
            return setRandomPatrolTarget(world, pos, tries + 1);
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

        return null;
    }

    // ✅ utilitaire weighted pick
    private static HordeMemberModConfig.WeaponConfig pickWeightedWeapon(Random random, List<HordeMemberModConfig.WeaponConfig> weapons) {
        int totalWeight = 0;
        for (HordeMemberModConfig.WeaponConfig w : weapons) {
            totalWeight += Math.max(0, w.getEffectiveWeight());
        }
        if (totalWeight <= 0) return null;

        int r = random.nextInt(totalWeight);
        for (HordeMemberModConfig.WeaponConfig w : weapons) {
            r -= Math.max(0, w.getEffectiveWeight());
            if (r < 0) return w;
        }
        return weapons.get(0);
    }

    private static void equipWithGear(Entity entity, Random random, HordeMemberModConfig.HordeComposition composition, int difficultyLevel) {
        if (!(entity instanceof LivingEntity living)) return;

        float armorChance = 0.06f * difficultyLevel;

        if (random.nextFloat() < armorChance) living.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        if (random.nextFloat() < armorChance) living.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        if (random.nextFloat() < armorChance) living.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        if (random.nextFloat() < armorChance) living.equipStack(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));

        // --- PILLAGER / VINDICATOR ---
        if (entity.getType() == EntityType.PILLAGER || entity.getType() == EntityType.VINDICATOR) {
            HordeMemberModConfig.HordeMobType mobType = getMobTypeFromComposition(entity, composition);
            if (mobType != null && mobType.weapons != null && !mobType.weapons.isEmpty()) {
                var picked = pickWeightedWeapon(random, mobType.weapons);
                if (picked != null) {
                    ItemStack weaponStack = new ItemStack(Registries.ITEM.get(Identifier.of(picked.itemId)));
                    living.equipStack(EquipmentSlot.MAINHAND, weaponStack);
                    return;
                }
            }

            if (entity.getType() == EntityType.PILLAGER) {
                living.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            } else {
                living.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            }
            return;
        }

        // --- Autres mobs ---
        HordeMemberModConfig.HordeMobType mobType = getMobTypeFromComposition(entity, composition);
        if (mobType != null && mobType.weapons != null && !mobType.weapons.isEmpty()) {
            if (random.nextFloat() < mobType.spawnWithWeaponProbability) {
                var picked = pickWeightedWeapon(random, mobType.weapons);
                if (picked != null) {
                    ItemStack weaponStack = new ItemStack(Registries.ITEM.get(Identifier.of(picked.itemId)));
                    living.equipStack(EquipmentSlot.MAINHAND, weaponStack);
                }
            }
        }
    }
}