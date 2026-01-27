package net.mebahel.zombiehorde.config;

import net.mebahel.zombiehorde.MebahelZombieHorde;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Generates a README.txt explaining the Zombie Horde configuration files.
 *
 * Files documented:
 *  - <modid>_config.json              (ZombieHordeModConfig)
 *  - <modid>mob_type_config.json      (HordeMemberModConfig)
 *
 * The README is generated in the same config directory.
 */
public class ZombieHordeConfigReadmeGenerator {

    private static final String README_FILE_NAME = "README.txt";

    public static void generate(File configDir) {
        if (!configDir.exists() && !configDir.mkdirs()) {
            System.err.println("[Zombie Horde] Failed to create config directory for README");
            return;
        }

        File readmeFile = new File(configDir, README_FILE_NAME);

        try (FileWriter writer = new FileWriter(readmeFile)) {
            String modId = MebahelZombieHorde.MOD_ID;

            String mainConfigFile = modId + "_config.json";
            String mobTypesConfigFile = modId + "mob_type_config.json"; // matches your current code

            StringBuilder sb = new StringBuilder();

            sb.append("Mebahel's Zombie Horde - Configuration Guide\n");
            sb.append("===========================================\n\n");
            sb.append("This README explains what each field in the configuration JSON files does.\n");
            sb.append("All config files are located in this folder.\n\n");

            sb.append("Files:\n");
            sb.append("  1) ").append(mainConfigFile).append("\n");
            sb.append("  2) ").append(mobTypesConfigFile).append("\n\n");

            // ------------------------------------------------------------
            // 1) main config
            // ------------------------------------------------------------
            sb.append("1) ").append(mainConfigFile).append("\n");
            sb.append("--------------------------------------------\n");
            sb.append("General Horde behaviour settings.\n\n");

            sb.append("  - spawnInDayLight (boolean, default: true)\n");
            sb.append("    If true, hordes can spawn during the day.\n");
            sb.append("    If false, hordes spawn only at night (Overworld).\n\n");

            sb.append("  - enableDifficultySystem (boolean, default: true)\n");
            sb.append("    If true, the mod can increase difficulty after the Nether has been reached.\n");
            sb.append("    (Your manager currently bumps difficulty once the advancement 'minecraft:nether/root' is completed.)\n\n");

            sb.append("  - hordeSpawning (boolean, default: true)\n");
            sb.append("    Master switch. If false, no hordes are spawned by the mod.\n\n");

            sb.append("  - hordeSpawnDelay (array [amount, unit], default: [15, \"minute\"])\n");
            sb.append("    Controls how often the mod attempts to spawn a horde.\n");
            sb.append("    Accepted units:\n");
            sb.append("      * \"minute\" => amount * 60 seconds\n");
            sb.append("      * \"day\"    => amount * 24000 ticks\n");
            sb.append("    Examples:\n");
            sb.append("      [15, \"minute\"] => every ~15 minutes\n");
            sb.append("      [1,  \"day\"]    => once per Minecraft day (random time in the day block)\n");
            sb.append("    Backward compatibility:\n");
            sb.append("      If the value was a number (legacy config), it is auto-converted to [number, \"minute\"].\n\n");

            sb.append("  - hordeSpawnChance (float 0..1, default: 1.0)\n");
            sb.append("    Probability for each horde attempt to actually spawn.\n");
            sb.append("    1.0 => always spawns when scheduled.\n");
            sb.append("    0.5 => ~50% of the attempts spawn.\n\n");

            sb.append("  - hordeNumber (int 1..20, default: 1)\n");
            sb.append("    Number of horde spawn attempts each time the schedule triggers.\n");
            sb.append("    Example: 3 means the manager will attempt spawning 3 hordes in a row (each gated by hordeSpawnChance).\n\n");

            sb.append("  - randomNumberHordeReinforcements (int 0..10, default: 0)\n");
            sb.append("    Adds a random extra amount of members to the horde.\n");
            sb.append("    If > 0, reinforcement = random(0..value-1) is added to the follower count.\n\n");

            sb.append("  - hordeMemberBonusHealth (int 0..800, default: 0)\n");
            sb.append("    Extra max health added to every spawned horde member.\n");
            sb.append("    Example: 10 => +10 HP on top of base mob health.\n\n");

            sb.append("  - hordeMemberBreakGlass (boolean, default: true)\n");
            sb.append("    If true, eligible mobs (from mob_type_config) get the goal to break glass.\n\n");

            sb.append("  - hordeMemberBreakFence (boolean, default: true)\n");
            sb.append("    If true, eligible mobs (from mob_type_config) get the goal to break fences.\n\n");

            sb.append("  - showHordeSpawningMessage (boolean, default: true)\n");
            sb.append("    If true, sends a chat message when a horde spawns.\n");
            sb.append("    The message includes clickable coordinates that run a /tp command.\n\n");

            sb.append("  - hordeSpawnDistanceFromPlayer (int >= 0, default: 60)\n");
            sb.append("    Base distance (in blocks) from the chosen player for spawning the horde.\n");
            sb.append("    Your code then adds a random extra offset (0..19) to X and Z.\n\n");

            sb.append("Validation rules applied at load:\n");
            sb.append("  - hordeSpawnChance must be within [0..1]\n");
            sb.append("  - hordeNumber must be within [1..20]\n");
            sb.append("  - randomNumberHordeReinforcements must be within [0..10]\n");
            sb.append("  - hordeMemberBonusHealth must be within [0..800]\n");
            sb.append("  - hordeSpawnDistanceFromPlayer must be >= 0\n\n");

            // ------------------------------------------------------------
            // 2) mob types config
            // ------------------------------------------------------------
            sb.append("2) ").append(mobTypesConfigFile).append("\n");
            sb.append("--------------------------------------------\n");
            sb.append("Defines which mobs can appear in a horde, where they can spawn, and what weapons they may get.\n\n");

            sb.append("Root:\n");
            sb.append("  {\n");
            sb.append("    \"hordeCompositions\": [ ... ]\n");
            sb.append("  }\n\n");

            sb.append("hordeCompositions[] fields:\n");
            sb.append("  - weight (int > 0)\n");
            sb.append("    Weight used to randomly pick a composition.\n");
            sb.append("    Higher weight => more likely selection.\n\n");

            sb.append("  - dimensions (string[] of dimension ids)\n");
            sb.append("    List of dimension identifiers in which this composition is eligible.\n");
            sb.append("    Example: [\"minecraft:overworld\", \"minecraft:the_nether\"]\n");
            sb.append("    If missing/empty, the loader auto-fills [\"minecraft:overworld\"].\n\n");

            sb.append("  - mobTypes (array)\n");
            sb.append("    List of mobs that can spawn for this composition.\n\n");

            sb.append("mobTypes[] fields:\n");
            sb.append("  - id (string)\n");
            sb.append("    Entity type identifier. Example: \"minecraft:zombie\".\n\n");

            sb.append("  - weight (int > 0)\n");
            sb.append("    Weight used to pick which mob type is spawned inside the composition.\n\n");

            sb.append("  - spawnWithWeaponProbability (float, default: 0.15)\n");
            sb.append("    Probability (0..1) that the mob spawns with a weapon from 'weapons'.\n");
            sb.append("    If <= 0 in config, the loader forces it to 0.15.\n\n");

            sb.append("  - weapons (array, optional)\n");
            sb.append("    Weapon pool used when the mob spawns with a weapon.\n");
            sb.append("    Each entry is weighted.\n\n");

            sb.append("weapons[] fields:\n");
            sb.append("  - itemId (string)\n");
            sb.append("    Item identifier. Example: \"minecraft:iron_sword\".\n\n");

            sb.append("  - weight (int, preferred)\n");
            sb.append("    Relative weight for random pick.\n");
            sb.append("    Higher weight => more likely.\n");
            sb.append("    Invalid weights (<= 0) are fixed to 1 by the loader.\n\n");

            sb.append("  - chance (float, legacy compatibility)\n");
            sb.append("    Old field kept for backward compatibility.\n");
            sb.append("    If 'weight' is not valid, 'chance' (0..1) is converted into a weight ~ (chance*100).\n\n");

            writer.write(sb.toString());
            System.out.println("[Zombie Horde] Generated config README at: " + readmeFile.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("[Zombie Horde] Failed to write config README: " + e.getMessage());
        }
    }
}