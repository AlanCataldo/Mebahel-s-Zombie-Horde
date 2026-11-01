package net.mebahel.zombiehorde;

import net.fabricmc.api.ClientModInitializer;

public class MebahelZombieHordeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        net.mebahelsapi.client.config.MebahelConfigRegistry.register(MebahelZombieHorde.MOD_ID,
                net.mebahel.zombiehorde.client.HordeConfigScreen::new,
                net.mebahel.zombiehorde.config.HordeConfigEditor.CODEC);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                net.mebahel.zombiehorde.config.HordeConfigSync.Snapshot.ID,
                (payload, context) -> context.client().execute(() ->
                        net.mebahel.zombiehorde.config.HordeConfigEditor.serverSnapshot = payload.json()));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> net.mebahel.zombiehorde.config.HordeConfigEditor.serverSnapshot = null);
    }
}
