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
            new HordeComposition(
                    1,
                    List.of("minecraft:overworld"), // ✅ par défaut : overworld
                    List.of(
                            new HordeMobType("minecraft:zombie",
                                    30,
                                    0.15f,
                                    List.of(
                                            new WeaponConfig("minecraft:iron_sword", 0.5f),
                                            new WeaponConfig("minecraft:stone_sword", 0.3f),
                                            new WeaponConfig("minecraft:wooden_sword", 0.2f)
                                    )
                            )
                    )
            )
    );

    public static void loadConfig(File configDir) {
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File configFile = new File(configDir, CONFIG_FILE_NAME);
        boolean updated = false;

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                ConfigData data = GSON.fromJson(reader, ConfigData.class);

                if (data == null || data.hordeCompositions == null) {
                    System.err.println("[Mebahel's Zombie Horde] Config file is malformed. Using default configuration.");
                    data = createDefaultConfig();
                    updated = true;
                } else if (data.hordeCompositions.isEmpty()) {
                    System.err.println("[Mebahel's Zombie Horde] Config file is empty. Skipping loading. Please define at least one horde composition.");
                    return;
                }

                hordeCompositions = data.hordeCompositions;

                // Correction des champs manquants
                for (HordeComposition composition : data.hordeCompositions) {
                    // ✅ Ajoute dimensions par défaut si manquantes
                    if (composition.dimensions == null || composition.dimensions.isEmpty()) {
                        composition.dimensions = List.of("minecraft:overworld");
                        updated = true;
                        System.out.println("[Mebahel's Zombie Horde] Added default dimension 'minecraft:overworld' to a horde composition");
                    }

                    for (HordeMobType mobType : composition.mobTypes) {
                        if (mobType.spawnWithWeaponProbability <= 0) {
                            mobType.spawnWithWeaponProbability = 0.15f;
                            updated = true;
                            System.out.println("[Mebahel's Zombie Horde] Added missing spawnWithWeaponProbability=0.15 to " + mobType.id);
                        }
                    }
                }

                // ✅ Journalisation détaillée
                System.out.println("-------------------[MEBAHEL'S ZOMBIE HORDE]-------------------");
                System.out.println("[Mebahel's Zombie Horde] Loaded " + hordeCompositions.size() + " horde composition(s):");

                int hordeIndex = 1;
                for (HordeComposition composition : hordeCompositions) {
                    System.out.println("  Horde #" + hordeIndex + " (weight: " + composition.weight + "): Dimensions=" + composition.dimensions);

                    for (HordeMobType mobType : composition.mobTypes) {
                        System.out.println("    - " + mobType.id + " (weight: " + mobType.weight + ")");
                        if (mobType.weapons != null && !mobType.weapons.isEmpty()) {
                            for (WeaponConfig weapon : mobType.weapons) {
                                System.out.println("        * Weapon: " + weapon.itemId + " (chance: " + (weapon.chance * 100) + "%)");
                            }
                        }
                    }
                    hordeIndex++;
                }
                System.out.println("-------------------[MEBAHEL'S ZOMBIE HORDE]-------------------");
            } catch (Exception e) {
                System.err.println("[Mebahel's Zombie Horde] Failed to load config file. Using default configuration: " + e.getMessage());
                hordeCompositions = createDefaultConfig().hordeCompositions;
                updated = true;
            }
        } else {
            System.out.println("[Mebahel's Zombie Horde] Config file not found. Creating a new one with default configuration.");
            hordeCompositions = createDefaultConfig().hordeCompositions;
            updated = true;
        }

        if (updated) {
            saveConfig(configDir);
        }
    }


    private static ConfigData createDefaultConfig() {
        return new ConfigData(List.of(
                new HordeComposition(
                        1,
                        List.of("minecraft:overworld"),
                        List.of(
                                new HordeMobType("minecraft:zombie",
                                        30,
                                        0.15f,
                                        List.of(
                                                new WeaponConfig("minecraft:iron_sword", 0.65f),
                                                new WeaponConfig("minecraft:stone_sword", 0.35f)
                                        )
                                )
                        )
                )
        ));
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
        public int weight;
        public List<String> dimensions; // ✅ Nouvel attribut
        public List<HordeMobType> mobTypes;

        HordeComposition(int weight, List<String> dimensions, List<HordeMobType> mobTypes) {
            this.weight = weight;
            this.dimensions = dimensions;
            this.mobTypes = mobTypes;
        }
    }

    // Classe représentant un type de mob avec un poids et un tableau d'armes
    public static class HordeMobType {
        public String id;
        public int weight;
        public float spawnWithWeaponProbability;
        public List<WeaponConfig> weapons;

        HordeMobType(String id, int weight, float spawnWithWeaponProbability, List<WeaponConfig> weapons) {
            this.id = id;
            this.weight = weight;
            this.spawnWithWeaponProbability = spawnWithWeaponProbability;
            this.weapons = weapons;
        }
    }

    // Classe représentant une arme avec sa probabilité
    public static class WeaponConfig {
        public String itemId;
        public float chance;

        WeaponConfig(String itemId, float chance) {
            this.itemId = itemId;
            this.chance = chance;
        }
    }
}
