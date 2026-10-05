## Mebahel's Zombie Horde - v2.0.0 - 1.21.1

### Minecraft 1.21.1

## What's New

- Overworld hordes now match the biome where their leader spawns. Deserts and badlands spawn husks; other biomes spawn zombies.
- Rare elite hordes can spawn with weapons and armor. Regular hordes remain more common.
- Custom horde compositions can be restricted to specific biomes. When a matching biome-specific composition exists, it takes priority.
- Armor is more customizable: each mob type can define an armor spawn chance and weighted item lists for each armor slot.

## Horde Spawning

- In the Overworld, hordes look for a surface position with solid ground, enough space, and an unobstructed view of the sky. If no suitable position is found, the spawn attempt is skipped.
- Members of a biome-specific horde spawn in the same biome as their leader.

## Configuration

- Newly generated default and example configurations include zombie, husk, and elite horde compositions.
- Existing configurations remain usable: without a biome restriction, a composition can spawn in any allowed biome; without armor settings, the existing random iron armor behavior applies.
