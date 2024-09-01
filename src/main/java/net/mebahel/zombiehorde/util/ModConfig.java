package net.mebahel.zombiehorde.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.mebahel.zombiehorde.MebahelZombieHorde;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfig {
    private static final String CONFIG_FILE_NAME = MebahelZombieHorde.MOD_ID + "_config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static boolean spawnInDaylight = true;
    public static boolean enableDifficultySystem = true;
    public static boolean patrolSpawning = true;
    public static int patrolSpawnDelay = 20;
    public static float hordeSpawnChance = 0.8f;
    public static int randomNumberHordeReinforcements = 0;
    public static int hordeNumber = 1;
    public static int hordeMemberBonusHealth = 0;


    public static void loadConfig(File configDir) {
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File configFile = new File(configDir, CONFIG_FILE_NAME);
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                ConfigData data = GSON.fromJson(reader, ConfigData.class);

                boolean updated = false;

                if (data.spawnInDayLight == null) {
                    data.spawnInDayLight = true;
                    updated = true;
                }
                if (data.enableDifficultySystem == null) {
                    data.enableDifficultySystem = true;
                    updated = true;
                }
                if (data.hordeSpawning == null) {
                    data.hordeSpawning = true;
                    updated = true;
                }
                if (data.hordeSpawnDelay == null || data.hordeSpawnDelay < 1 || data.hordeSpawnDelay > 60) {
                    data.hordeSpawnDelay = 20;
                    updated = true;
                }
                if (data.hordeSpawnChance == null || data.hordeSpawnChance < 0 || data.hordeSpawnChance > 1) {
                    data.hordeSpawnChance = 0.8f;
                    updated = true;
                }

                if (data.randomNumberHordeReinforcements == null || data.randomNumberHordeReinforcements < 0
                        || data.randomNumberHordeReinforcements > 10) {
                    data.randomNumberHordeReinforcements = 0;
                    updated = true;
                }

                if (data.hordeNumber == null || data.hordeNumber < 1
                        || data.hordeNumber > 5) {
                    data.hordeNumber = 1;
                    updated = true;
                }

                if (data.hordeMemberBonusHealth == null || data.hordeMemberBonusHealth < 0
                        || data.hordeMemberBonusHealth > 40) {
                    data.hordeMemberBonusHealth = 0;
                    updated = true;
                }

                spawnInDaylight = data.spawnInDayLight;
                enableDifficultySystem = data.enableDifficultySystem;
                patrolSpawning = data.hordeSpawning;
                patrolSpawnDelay = data.hordeSpawnDelay;
                hordeSpawnChance = data.hordeSpawnChance;
                randomNumberHordeReinforcements = data.randomNumberHordeReinforcements;
                hordeNumber = data.hordeNumber;
                hordeMemberBonusHealth = data.hordeMemberBonusHealth;


                if (updated) {
                    saveConfig(configDir);
                }
            } catch (IOException e) {
                System.err.println("Failed to load config file: " + e.getMessage());
            }
        } else {
            saveConfig(configDir);
        }
    }

    public static void saveConfig(File configDir) {
        File configFile = new File(configDir, CONFIG_FILE_NAME);
        ConfigData data = new ConfigData(spawnInDaylight ,enableDifficultySystem, patrolSpawning, patrolSpawnDelay, hordeSpawnChance,
                randomNumberHordeReinforcements, hordeNumber, hordeMemberBonusHealth);
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
        Integer hordeSpawnDelay;
        Float hordeSpawnChance;
        Integer randomNumberHordeReinforcements;
        Integer hordeNumber;
        Integer hordeMemberBonusHealth;

        ConfigData(boolean spawnInDayLight, boolean enableDifficultySystem, boolean hordeSpawning, int hordeSpawnDelay,
                   float hordeSpawnChance, int randomNumberHordeReinforcements, int hordeNumber, int hordeMemberBonusHealth) {
            this.spawnInDayLight = spawnInDayLight;
            this.enableDifficultySystem = enableDifficultySystem;
            this.hordeSpawning = hordeSpawning;
            this.hordeSpawnDelay = hordeSpawnDelay;
            this.hordeSpawnChance = hordeSpawnChance;
            this.randomNumberHordeReinforcements = randomNumberHordeReinforcements;
            this.hordeNumber = hordeNumber;
            this.hordeMemberBonusHealth = hordeMemberBonusHealth;
        }
    }
}