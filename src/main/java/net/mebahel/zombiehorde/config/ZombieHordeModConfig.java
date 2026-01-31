package net.mebahel.zombiehorde.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.mebahel.zombiehorde.MebahelZombieHorde;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ZombieHordeModConfig {
    private static final String CONFIG_FILE_NAME = MebahelZombieHorde.MOD_ID + "_config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean spawnInDaylight = true;
    public static boolean enableDifficultySystem = true;
    public static boolean patrolSpawning = true;

    // ✅ Nouveau format : [15, "minute"]
    public static int hordeSpawnDelayAmount = 15;
    public static String hordeSpawnDelayUnit = "minute";

    public static float hordeSpawnChance = 1f;
    public static int randomNumberHordeReinforcements = 0;
    public static int hordeNumber = 1;
    public static int hordeMemberBonusHealth = 0;
    public static boolean hordeMemberBreakGlass = true;
    public static boolean hordeMemberBreakFence = true;
    public static boolean showHordeSpawningMessage = true;

    public static int hordeSpawnDistanceFromPlayer = 60;

    public static void loadConfig(File configDir) {
        if (!configDir.exists()) configDir.mkdirs();

        File configFile = new File(configDir, CONFIG_FILE_NAME);
        boolean updated = false;

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                ConfigData data = GSON.fromJson(reader, ConfigData.class);

                if (data.spawnInDayLight == null) { data.spawnInDayLight = true; updated = true; }
                if (data.enableDifficultySystem == null) { data.enableDifficultySystem = true; updated = true; }
                if (data.hordeSpawning == null) { data.hordeSpawning = true; updated = true; }

                // ✅ Vérification du format [int, string]
                if (data.hordeSpawnDelay == null) {
                    data.hordeSpawnDelay = List.of(15, "minute");
                    updated = true;
                } else if (data.hordeSpawnDelay instanceof Number) {
                    // Ancien format → transformation automatique
                    int legacyValue = ((Number) data.hordeSpawnDelay).intValue();
                    data.hordeSpawnDelay = List.of(legacyValue, "minute");
                    hordeSpawnDelayAmount = legacyValue;
                    hordeSpawnDelayUnit = "minute";
                    updated = true;
                } else if (data.hordeSpawnDelay instanceof List<?> list && list.size() == 2) {
                    try {
                        Object amount = list.get(0);
                        Object unit = list.get(1);

                        if (!(amount instanceof Number) || !(unit instanceof String)) throw new IllegalArgumentException();

                        int amt = ((Number) amount).intValue();
                        String unt = ((String) unit).toLowerCase();

                        if (amt < 1 || (!unt.equals("minute") && !unt.equals("day"))) throw new IllegalArgumentException();

                        hordeSpawnDelayAmount = amt;
                        hordeSpawnDelayUnit = unt;
                    } catch (Exception e) {
                        System.err.println("[Zombie Horde] Invalid 'hordeSpawnDelay' format. Resetting to [15, \"minute\"]");
                        data.hordeSpawnDelay = List.of(15, "minute");
                        hordeSpawnDelayAmount = 15;
                        hordeSpawnDelayUnit = "minute";
                        updated = true;
                    }
                } else {
                    // Format complètement invalide
                    System.err.println("[Zombie Horde] Invalid 'hordeSpawnDelay' value. Resetting.");
                    data.hordeSpawnDelay = List.of(15, "minute");
                    hordeSpawnDelayAmount = 15;
                    hordeSpawnDelayUnit = "minute";
                    updated = true;
                }


                if (data.hordeSpawnChance == null || data.hordeSpawnChance < 0 || data.hordeSpawnChance > 1) {
                    data.hordeSpawnChance = 1f;
                    updated = true;
                }

                if (data.randomNumberHordeReinforcements == null || data.randomNumberHordeReinforcements < 0 || data.randomNumberHordeReinforcements > 10) {
                    data.randomNumberHordeReinforcements = 0;
                    updated = true;
                }

                if (data.hordeNumber == null || data.hordeNumber < 1 || data.hordeNumber > 20) {
                    data.hordeNumber = 1;
                    updated = true;
                }

                if (data.hordeMemberBonusHealth == null || data.hordeMemberBonusHealth < 0 ||
                        data.hordeMemberBonusHealth > 800) {
                    data.hordeMemberBonusHealth = 0;
                    updated = true;
                }

                if (data.hordeSpawnDistanceFromPlayer == null || data.hordeSpawnDistanceFromPlayer < 0) {
                    data.hordeSpawnDistanceFromPlayer = 40;
                    updated = true;
                }

                if (data.hordeMemberBreakGlass == null) { data.hordeMemberBreakGlass = true; updated = true; }
                if (data.hordeMemberBreakFence == null) { data.hordeMemberBreakFence = true; updated = true; }
                if (data.showHordeSpawningMessage == null) { data.showHordeSpawningMessage = true; updated = true; }

                spawnInDaylight = data.spawnInDayLight;
                enableDifficultySystem = data.enableDifficultySystem;
                patrolSpawning = data.hordeSpawning;
                hordeSpawnChance = data.hordeSpawnChance;
                randomNumberHordeReinforcements = data.randomNumberHordeReinforcements;
                hordeNumber = data.hordeNumber;
                hordeMemberBonusHealth = data.hordeMemberBonusHealth;
                hordeMemberBreakGlass = data.hordeMemberBreakGlass;
                hordeMemberBreakFence = data.hordeMemberBreakFence;
                showHordeSpawningMessage = data.showHordeSpawningMessage;
                hordeSpawnDistanceFromPlayer = data.hordeSpawnDistanceFromPlayer;

                if (updated) saveConfig(configDir);

            } catch (IOException e) {
                System.err.println("Failed to load config file: " + e.getMessage());
            }
        } else {
            saveConfig(configDir);
        }
    }

    public static void saveConfig(File configDir) {
        File configFile = new File(configDir, CONFIG_FILE_NAME);
        ConfigData data = new ConfigData(
                spawnInDaylight,
                enableDifficultySystem,
                patrolSpawning,
                List.of(hordeSpawnDelayAmount, hordeSpawnDelayUnit),
                hordeSpawnChance,
                randomNumberHordeReinforcements,
                hordeNumber,
                hordeMemberBonusHealth,
                hordeMemberBreakGlass,
                hordeMemberBreakFence,
                showHordeSpawningMessage,
                hordeSpawnDistanceFromPlayer
        );
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("Failed to save config file: " + e.getMessage());
        }
    }

    private static class ConfigData {
        Boolean spawnInDayLight;
        Boolean enableDifficultySystem;
        Boolean hordeSpawning;
        Object  hordeSpawnDelay;
        Float hordeSpawnChance;
        Integer randomNumberHordeReinforcements;
        Integer hordeNumber;
        Integer hordeMemberBonusHealth;
        Boolean hordeMemberBreakGlass;
        Boolean hordeMemberBreakFence;
        Boolean showHordeSpawningMessage;
        Integer hordeSpawnDistanceFromPlayer;

        ConfigData(Boolean spawnInDayLight, Boolean enableDifficultySystem, Boolean hordeSpawning,
                   List<Object> hordeSpawnDelay, Float hordeSpawnChance,
                   Integer randomNumberHordeReinforcements, Integer hordeNumber, Integer hordeMemberBonusHealth,
                   Boolean hordeMemberBreakGlass, Boolean hordeMemberBreakFence, Boolean showHordeSpawningMessage,
                   Integer hordeSpawnDistanceFromPlayer) {
            this.spawnInDayLight = spawnInDayLight;
            this.enableDifficultySystem = enableDifficultySystem;
            this.hordeSpawning = hordeSpawning;
            this.hordeSpawnDelay = hordeSpawnDelay;
            this.hordeSpawnChance = hordeSpawnChance;
            this.randomNumberHordeReinforcements = randomNumberHordeReinforcements;
            this.hordeNumber = hordeNumber;
            this.hordeMemberBonusHealth = hordeMemberBonusHealth;
            this.hordeMemberBreakGlass = hordeMemberBreakGlass;
            this.hordeMemberBreakFence = hordeMemberBreakFence;
            this.showHordeSpawningMessage = showHordeSpawningMessage;
            this.hordeSpawnDistanceFromPlayer = hordeSpawnDistanceFromPlayer;
        }
    }

    public static class HordeDelayResult {
        public final long ticks;
        public final String unit;

        public HordeDelayResult(long ticks, String unit) {
            this.ticks = ticks;
            this.unit = unit;
        }
    }

    public static HordeDelayResult getHordeDelayInTicksWithUnit() {
        int base = hordeSpawnDelayAmount;
        String unit = hordeSpawnDelayUnit;

        return switch (unit) {
            case "minute", "minutes" -> new HordeDelayResult(base * 20 * 60L, "minute");
            case "day", "days" -> new HordeDelayResult(base * 24000L, "day");
            default -> new HordeDelayResult(15 * 20 * 60L, "minute");
        };
    }
}