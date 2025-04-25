package net.mebahel.zombiehorde;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.mebahel.zombiehorde.util.HordeMemberModConfig;
import net.mebahel.zombiehorde.util.ModConfig;
import net.mebahel.zombiehorde.util.ZombieHordeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;


public class MebahelZombieHorde implements ModInitializer {
	public static final String MOD_ID = "mebahel-zombie-horde";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		File configDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), MebahelZombieHorde.MOD_ID);
		ModConfig.loadConfig(configDir);
		HordeMemberModConfig.loadConfig(configDir);
		ZombieHordeManager.register();
		LOGGER.info("[Mebahel's Zombie Horde] Initialization complete.");
	}
}