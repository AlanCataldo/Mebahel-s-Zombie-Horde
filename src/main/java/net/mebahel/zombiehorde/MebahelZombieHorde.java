package net.mebahel.zombiehorde;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.mebahel.zombiehorde.command.HordeInfoCommand;
import net.mebahel.zombiehorde.command.ReloadConfigCommand;
import net.mebahel.zombiehorde.command.SpawnHordeCommand;
import net.mebahel.zombiehorde.config.HordeExampleConfigGenerator;
import net.mebahel.zombiehorde.config.HordeMemberModConfig;
import net.mebahel.zombiehorde.config.ZombieHordeConfigReadmeGenerator;
import net.mebahel.zombiehorde.util.ZombieHordeManager;
import net.mebahel.zombiehorde.config.ZombieHordeModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class MebahelZombieHorde implements ModInitializer {
	public static final String MOD_ID = "mebahel-zombie-horde";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		File configDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), MebahelZombieHorde.MOD_ID);
		ZombieHordeModConfig.loadConfig(configDir);
		HordeMemberModConfig.loadConfig(configDir);
		HordeExampleConfigGenerator.generate(configDir);
		ZombieHordeConfigReadmeGenerator.generate(configDir);
		ZombieHordeManager.register();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			SpawnHordeCommand.register(dispatcher);
			ReloadConfigCommand.register(dispatcher);
			HordeInfoCommand.register(dispatcher);
		});
		LOGGER.info("[Mebahel's Zombie Horde] Initialization complete.");
	}
}