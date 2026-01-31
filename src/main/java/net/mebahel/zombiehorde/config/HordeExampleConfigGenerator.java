package net.mebahel.zombiehorde.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.mebahel.zombiehorde.config.HordeMemberModConfig.HordeComposition;
import net.mebahel.zombiehorde.config.HordeMemberModConfig.HordeMobType;
import net.mebahel.zombiehorde.config.HordeMemberModConfig.WeaponConfig;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class HordeExampleConfigGenerator {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String EXAMPLE_FILE_NAME = "horde_example.json";

    public static void generate(File configDir) {
        if (!configDir.exists() && !configDir.mkdirs()) {
            System.err.println("[Mebahel's Zombie Horde] Failed to create config directory.");
            return;
        }

        File exampleFile = new File(configDir, EXAMPLE_FILE_NAME);

        // 1) If file exists: try to migrate legacy "chance" -> "weight"
        if (exampleFile.exists()) {
            boolean rewritten = tryRewriteIfContainsChance(exampleFile);
            if (!rewritten) {
                System.out.println("[Mebahel's Zombie Horde] '" + EXAMPLE_FILE_NAME + "' already exists, no migration needed.");
            }
            return;
        }

        // 2) Otherwise: create default example
        List<HordeComposition> exampleHordes = List.of(
                // Horde OVERWORLD
                new HordeComposition(1, List.of("minecraft:overworld"), List.of(
                        new HordeMobType("minecraft:zombie", 40, 0.8f, List.of(
                                new WeaponConfig("minecraft:iron_sword", 5),
                                new WeaponConfig("minecraft:stone_sword", 3),
                                new WeaponConfig("minecraft:wooden_sword", 2)
                        )),
                        new HordeMobType("minecraft:skeleton", 30, 1.0f, List.of(
                                new WeaponConfig("minecraft:bow", 3),
                                new WeaponConfig("minecraft:iron_axe", 2),
                                new WeaponConfig("minecraft:stone_axe", 2)
                        ))
                )),

                // Horde NETHER
                new HordeComposition(1, List.of("minecraft:the_nether"), List.of(
                        new HordeMobType("minecraft:pillager", 35, 1.0f, List.of(
                                new WeaponConfig("minecraft:crossbow", 1)
                        )),
                        new HordeMobType("minecraft:vindicator", 25, 1.0f, List.of(
                                new WeaponConfig("minecraft:iron_axe", 1)
                        )),
                        new HordeMobType("minecraft:evoker", 15, 0.0f, List.of()),
                        new HordeMobType("minecraft:ravager", 5, 0.0f, List.of())
                )),

                // Horde END
                new HordeComposition(2, List.of("minecraft:the_end"), List.of(
                        new HordeMobType("minecraft:wither_skeleton", 40, 0.7f, List.of(
                                new WeaponConfig("minecraft:stone_sword", 4),
                                new WeaponConfig("minecraft:iron_axe", 3)
                        )),
                        new HordeMobType("minecraft:stray", 20, 0.5f, List.of(
                                new WeaponConfig("minecraft:bow", 1)
                        )),
                        new HordeMobType("minecraft:blaze", 15, 0.0f, List.of()) // no weapon
                ))
        );

        try (FileWriter writer = new FileWriter(exampleFile)) {
            GSON.toJson(new ConfigData(exampleHordes), writer);
            System.out.println("[Mebahel's Zombie Horde] '" + EXAMPLE_FILE_NAME + "' successfully created.");
        } catch (IOException e) {
            System.err.println("[Mebahel's Zombie Horde] Failed to create '" + EXAMPLE_FILE_NAME + "': " + e.getMessage());
        }
    }

    /**
     * Reads existing horde_example.json and rewrites it if any WeaponConfig uses legacy "chance"
     * (or invalid weight). After rewrite: only "weight" remains (chance=null).
     *
     * @return true if file was rewritten, false otherwise
     */
    private static boolean tryRewriteIfContainsChance(File exampleFile) {
        ConfigData data;
        try (FileReader reader = new FileReader(exampleFile)) {
            data = GSON.fromJson(reader, ConfigData.class);
        } catch (Exception e) {
            System.err.println("[Mebahel's Zombie Horde] Failed to read '" + EXAMPLE_FILE_NAME + "' for migration: " + e.getMessage());
            return false;
        }

        if (data == null || data.hordeCompositions == null) return false;

        boolean needsRewrite = false;

        for (HordeComposition comp : data.hordeCompositions) {
            if (comp == null || comp.mobTypes == null) continue;

            for (HordeMobType mob : comp.mobTypes) {
                if (mob == null || mob.weapons == null) continue;

                for (WeaponConfig w : mob.weapons) {
                    if (w == null) continue;

                    // legacy chance present OR invalid weight => normalize
                    if (w.chance != null || w.weight <= 0) {
                        needsRewrite = true;

                        int normalized = w.getEffectiveWeight(); // your compat logic
                        w.weight = Math.max(1, normalized);
                        w.chance = null; // remove legacy field from JSON output
                    }
                }
            }
        }

        if (!needsRewrite) return false;

        try (FileWriter writer = new FileWriter(exampleFile, false)) {
            GSON.toJson(data, writer);
            System.out.println("[Mebahel's Zombie Horde] '" + EXAMPLE_FILE_NAME + "' contained legacy 'chance' -> rewritten with weights only.");
            return true;
        } catch (IOException e) {
            System.err.println("[Mebahel's Zombie Horde] Failed to rewrite '" + EXAMPLE_FILE_NAME + "': " + e.getMessage());
            return false;
        }
    }

    // JSON root object
    private static class ConfigData {
        List<HordeComposition> hordeCompositions;

        ConfigData(List<HordeComposition> hordeCompositions) {
            this.hordeCompositions = hordeCompositions;
        }
    }
}