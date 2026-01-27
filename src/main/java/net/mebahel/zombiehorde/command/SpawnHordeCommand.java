package net.mebahel.zombiehorde.command;

import com.mojang.brigadier.CommandDispatcher;
import net.mebahel.zombiehorde.util.ZombieHordeManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;

public class SpawnHordeCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("mebahelzombiehorde")
                        .requires(source -> source.hasPermissionLevel(2)) // opérateurs uniquement
                        .then(literal("spawnhorde")
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    if (source.getEntity() instanceof ServerPlayerEntity player) {
                                        BlockPos playerPos = player.getBlockPos();
                                        ZombieHordeManager.forceSpawnPatrol(player.getServerWorld(), playerPos, UUID.randomUUID());
                                        source.sendFeedback(() -> Text.literal("[Mebahel's Zombie Horde] Horde spawned near you!"), true);
                                        return 1;
                                    } else {
                                        source.sendError(Text.literal("Only players can use this command."));
                                        return 0;
                                    }
                                })
                        )
        );
    }
}