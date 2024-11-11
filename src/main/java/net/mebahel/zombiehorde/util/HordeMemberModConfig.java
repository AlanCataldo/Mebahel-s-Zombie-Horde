package net.mebahel.zombiehorde.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.mebahel.zombiehorde.MebahelZombieHorde;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class HordeMemberModConfig {
    private static final String CONFIG_FILE_NAME = MebahelZombieHorde.MOD_ID + "mob_type_config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static List<HordeComposition> hordeCompositions = List.of(
            new HordeComposition(List.of(
                    new HordeMobType("minecraft:zombie", 30)
            ))
    );

    public static void loadConfig(File configDir) {
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File configFile = new File(configDir, CONFIG_FILE_NAME);
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                ConfigData data = GSON.fromJson(reader, ConfigData.class);

                boolean updated = false;

                if (data.hordeCompositions == null || data.hordeCompositions.isEmpty()) {
                    data.hordeCompositions = List.of(
                            new HordeComposition(List.of(
                                    new HordeMobType("minecraft:zombie", 30)
                            ))
                    );
                    updated = true;
                }

                hordeCompositions = data.hordeCompositions;

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
        ConfigData data = new ConfigData(hordeCompositions);
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("Failed to save config file: " + e.getMessage());
        }
    }

    // Classe représentant la configuration complète
    private static class ConfigData {
        List<HordeComposition> hordeCompositions;

        ConfigData(List<HordeComposition> hordeCompositions) {
            this.hordeCompositions = hordeCompositions;
        }
    }

    // Classe représentant une composition de horde
    public static class HordeComposition {
        public List<HordeMobType> mobTypes;

        HordeComposition(List<HordeMobType> mobTypes) {
            this.mobTypes = mobTypes;
        }
    }

    // Classe représentant un type de mob avec un poids
    public static class HordeMobType {
        public String id;
        int weight;

        HordeMobType(String id, int weight) {
            this.id = id;
            this.weight = weight;
        }
    }
}