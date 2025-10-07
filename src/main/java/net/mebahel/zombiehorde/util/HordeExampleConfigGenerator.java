package net.mebahel.zombiehorde.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.mebahel.zombiehorde.util.HordeMemberModConfig.HordeComposition;
import net.mebahel.zombiehorde.util.HordeMemberModConfig.HordeMobType;
import net.mebahel.zombiehorde.util.HordeMemberModConfig.WeaponConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class HordeExampleConfigGenerator {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void generate(File configDir) {
        File exampleFile = new File(configDir, "horde_example.json");

        if (exampleFile.exists()) {
            System.out.println("[Mebahel's Zombie Horde] 'horde_example.json' already exists, skipping generation.");
            return;
        }

        List<HordeComposition> exampleHordes = List.of(
                // Horde OVERWORLD
                new HordeComposition(1, List.of("minecraft:overworld"), List.of(
                        new HordeMobType("minecraft:zombie", 40, 0.8f, List.of(
                                new WeaponConfig("minecraft:iron_sword", 0.5f),
                                new WeaponConfig("minecraft:stone_sword", 0.3f),
                                new WeaponConfig("minecraft:wooden_sword", 0.2f)
                        )),
                        new HordeMobType("minecraft:skeleton", 30, 1.0f, List.of(
                                new WeaponConfig("minecraft:bow", 0.3f),
                                new WeaponConfig("minecraft:iron_axe", 0.2f),
                                new WeaponConfig("minecraft:stone_axe", 0.2f)
                        ))
                )),

                // Horde NETHER
                new HordeComposition(1, List.of("minecraft:the_nether"), List.of(
                        new HordeMobType("minecraft:pillager", 35, 1.0f, List.of(
                                new WeaponConfig("minecraft:crossbow", 1.0f)
                        )),
                        new HordeMobType("minecraft:vindicator", 25, 1.0f, List.of(
                                new WeaponConfig("minecraft:iron_axe", 1.0f)
                        )),
                        new HordeMobType("minecraft:evoker", 15, 0.0f, List.of()),
                        new HordeMobType("minecraft:ravager", 5, 0.0f, List.of())
                )),

                // Horde END
                new HordeComposition(2, List.of("minecraft:the_end"), List.of(
                        new HordeMobType("minecraft:wither_skeleton", 40, 0.7f, List.of(
                                new WeaponConfig("minecraft:stone_sword", 0.4f),
                                new WeaponConfig("minecraft:iron_axe", 0.3f)
                        )),
                        new HordeMobType("minecraft:stray", 20, 0.5f, List.of(
                                new WeaponConfig("minecraft:bow", 1.0f)
                        )),
                        new HordeMobType("minecraft:blaze", 15, 0.0f, List.of()) // ne porte pas d'arme
                ))
        );

        try (FileWriter writer = new FileWriter(exampleFile)) {
            GSON.toJson(new ConfigData(exampleHordes), writer);
            System.out.println("[Mebahel's Zombie Horde] 'horde_example.json' successfully created.");
        } catch (IOException e) {
            System.err.println("[Mebahel's Zombie Horde] Failed to create 'horde_example.json': " + e.getMessage());
        }
    }

    // Classe interne pour sérialisation JSON
    private static class ConfigData {
        List<HordeComposition> hordeCompositions;

        ConfigData(List<HordeComposition> hordeCompositions) {
            this.hordeCompositions = hordeCompositions;
        }
    }
}