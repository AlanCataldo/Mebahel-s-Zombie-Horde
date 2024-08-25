package net.mebahel.zombiehorde;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.mebahel.zombiehorde.util.ModConfig;
import net.mebahel.zombiehorde.util.ZombieHordeManager;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.entity.mob.ZombieEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static net.mebahel.zombiehorde.entity.ModEntities.HUSK_HORDE;
import static net.mebahel.zombiehorde.entity.ModEntities.ZOMBIE_HORDE;

public class MebahelZombieHorde implements ModInitializer {
	public static final String MOD_ID = "mebahel-zombie-horde";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		File configDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), MebahelZombieHorde.MOD_ID);
		ModConfig.loadConfig(configDir);
		FabricDefaultAttributeRegistry.register(ZOMBIE_HORDE, ZombieEntity.createZombieAttributes());
		FabricDefaultAttributeRegistry.register(HUSK_HORDE, HuskEntity.createZombieAttributes());
		ZombieHordeManager.register();
		LOGGER.info("[Mebahel's Zombie Horde] Initialization complete.");
	}
}