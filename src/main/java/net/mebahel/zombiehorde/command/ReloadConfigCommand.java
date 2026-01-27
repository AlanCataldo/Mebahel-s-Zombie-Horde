package net.mebahel.zombiehorde.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.loader.api.FabricLoader;
import net.mebahel.zombiehorde.MebahelZombieHorde;
import net.mebahel.zombiehorde.config.HordeMemberModConfig;
import net.mebahel.zombiehorde.config.ZombieHordeModConfig;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.File;

import static net.minecraft.server.command.CommandManager.literal;

public class ReloadConfigCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("mebahelzombiehorde")
                        .requires(source -> source.hasPermissionLevel(2)) // opérateurs uniquement
                        .then(literal("reloadconfig")
                                .executes(context -> {
                                    File configDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), MebahelZombieHorde.MOD_ID);

                                    ZombieHordeModConfig.loadConfig(configDir);
                                    HordeMemberModConfig.loadConfig(configDir);

                                    context.getSource().sendFeedback(() ->
                                            Text.literal("[Mebahel's Zombie Horde] Configuration successfully reloaded."), true);

                                    return 1;
                                })
                        )
        );
    }
}