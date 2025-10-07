package net.mebahel.zombiehorde.command;

import com.mojang.brigadier.CommandDispatcher;
import net.mebahel.zombiehorde.util.ZombieHordeManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class HordeInfoCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("mebahelzombiehorde")
                .requires(source -> source.hasPermissionLevel(2)) // ✅ opérateurs uniquement
                .then(CommandManager.literal("nextspawn")
                        .executes(ctx -> {
                            ServerCommandSource source = ctx.getSource();
                            if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                                source.sendFeedback(() -> Text.literal("This command must be run by a player."), false);
                                return 1;
                            }

                            ServerWorld world = player.getServerWorld();
                            long currentTick = world.getTime();

                            // ✅ On récupère directement le tick planifié
                            Long scheduledTick = ZombieHordeManager.getNextScheduledHordeTick(world);

                            if (scheduledTick == null) {
                                player.sendMessage(Text.literal("[Mebahel's Zombie Horde] No horde spawn is currently scheduled."), false);
                                return 1;
                            }

                            long ticksRemaining = Math.max(0, scheduledTick - currentTick);
                            long secondsRemaining = ticksRemaining / 20;
                            long minutes = secondsRemaining / 60;
                            long seconds = secondsRemaining % 60;

                            player.sendMessage(Text.literal("[Mebahel's Zombie Horde] Next horde spawn in: ")
                                    .append(Text.literal(minutes + "m " + seconds + "s").formatted(Formatting.GOLD)), false);

                            return 1;
                        })
                )
        );
    }
}
