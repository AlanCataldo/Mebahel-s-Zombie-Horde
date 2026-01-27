package net.mebahel.zombiehorde.config;

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
                                            new WeaponConfig("minecraft:iron_sword", 20),
                                            new WeaponConfig("minecraft:stone_sword", 10),
                                            new WeaponConfig("minecraft:wooden_sword", 10)
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

                        // ✅ correction weights invalides + compat chance->weight
                        if (mobType.weapons != null) {
                            for (WeaponConfig w : mobType.weapons) {
                                if (w.getEffectiveWeight() <= 0) {
                                    w.weight = 1;
                                    updated = true;
                                    System.out.println("[Mebahel's Zombie Horde] Fixed invalid weapon weight for " + w.itemId);
                                }
                            }
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
                        System.out.println("    - " + mobType.id + " (weight: " + mobType.weight + ", spawnWithWeaponProbability=" + mobType.spawnWithWeaponProbability + ")");

                        if (mobType.weapons != null && !mobType.weapons.isEmpty()) {
                            int sum = mobType.weapons.stream().mapToInt(WeaponConfig::getEffectiveWeight).sum();
                            for (WeaponConfig weapon : mobType.weapons) {
                                double pct = (weapon.getEffectiveWeight() * 100.0) / Math.max(1, sum);
                                System.out.printf("        * Weapon: %s (weight: %d ~ %.1f%%)%n",
                                        weapon.itemId, weapon.getEffectiveWeight(), pct);
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
                                                new WeaponConfig("minecraft:iron_sword", 6),
                                                new WeaponConfig("minecraft:stone_sword", 4)
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

    // ✅ Classe représentant une arme avec un poids (et compat avec l'ancien champ "chance")
    public static class WeaponConfig {
        public String itemId;

        // Nouveau champ (préféré)
        public int weight = 1;

        // Ancien champ (compat JSON existant)
        public Float chance = null;

        public int getEffectiveWeight() {
            if (weight > 0) return weight;

            // Compat: convertit chance (0..1) en poids (1..100+)
            if (chance != null && chance > 0) {
                return Math.max(1, Math.round(chance * 100f));
            }

            return 1;
        }

        WeaponConfig(String itemId, int weight) {
            this.itemId = itemId;
            this.weight = weight;
        }

        // Optionnel: garde un ctor float si tu l'utilises encore quelque part
        WeaponConfig(String itemId, float chance) {
            this.itemId = itemId;
            this.chance = chance;
            this.weight = 0; // forcera la conversion via getEffectiveWeight()
        }
    }
}