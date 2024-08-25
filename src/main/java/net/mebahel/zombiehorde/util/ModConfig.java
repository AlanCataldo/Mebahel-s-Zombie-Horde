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

                spawnInDaylight = data.spawnInDayLight;
                enableDifficultySystem = data.enableDifficultySystem;
                patrolSpawning = data.hordeSpawning;
                patrolSpawnDelay = data.hordeSpawnDelay;


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
        ConfigData data = new ConfigData(spawnInDaylight ,enableDifficultySystem, patrolSpawning, patrolSpawnDelay);
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

        ConfigData(boolean spawnInDayLight, boolean enableDifficultySystem, boolean hordeSpawning, int hordeSpawnDelay) {
            this.spawnInDayLight = spawnInDayLight;
            this.enableDifficultySystem = enableDifficultySystem;
            this.hordeSpawning = hordeSpawning;
            this.hordeSpawnDelay = hordeSpawnDelay;
        }
    }
}
