package net.mebahel.zombiehorde.config;

import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.mebahel.zombiehorde.MebahelZombieHorde;

public final class HordeConfigSync {
    static MinecraftServer server;
    public record Snapshot(String json) implements CustomPayload {
        public static final Id<Snapshot> ID = new Id<>(Identifier.of(MebahelZombieHorde.MOD_ID,"config_snapshot"));
        public static final PacketCodec<RegistryByteBuf, Snapshot> CODEC = PacketCodec.tuple(PacketCodecs.string(1000000), Snapshot::json, Snapshot::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    public static void register() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(s -> server=s);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s -> server=null);
        PayloadTypeRegistry.playS2C().register(Snapshot.ID,Snapshot.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server) -> send(handler.player));
    }
    private static void send(ServerPlayerEntity player) {
        if (ServerPlayNetworking.canSend(player,Snapshot.ID)) ServerPlayNetworking.send(player,new Snapshot(new HordeConfigEditor().current()));
    }
    public static void syncAll(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) send(p);
    }
}
