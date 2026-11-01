package net.mebahel.zombiehorde.client;

import com.google.gson.*;
import net.mebahel.zombiehorde.config.HordeConfigEditor;
import net.mebahel.zombiehorde.MebahelZombieHorde;
import net.mebahelsapi.client.config.MebahelConfigClient;
import net.minecraft.client.MinecraftClient;
import java.util.function.Consumer;

final class HordeEditorDraft {
    final String original=HordeConfigEditor.serverSnapshot;
    final JsonObject root=original==null?HordeConfigEditor.snapshot():JsonParser.parseString(original).getAsJsonObject();
    boolean saving;
    boolean dirty() {return original!=null && !root.equals(JsonParser.parseString(original));}
    boolean canSave() {return original!=null && !saving && MebahelConfigClient.canManageConfigs();}
    void save(Consumer<String> status,Runnable success) {
        try {HordeConfigEditor.check(root);} catch(RuntimeException e) {status.accept(e.getMessage());return;}
        if(!canSave()) {status.accept("Connectez-vous à un monde avec les droits opérateur.");return;}
        saving=true;status.accept("Enregistrement sur le serveur…");
        MebahelConfigClient.submit(MebahelZombieHorde.MOD_ID,HordeConfigEditor.GSON.toJson(root)).whenComplete((result,error) -> MinecraftClient.getInstance().execute(() -> {
            saving=false;
            if(error!=null) status.accept("Enregistrement impossible. Le brouillon est conservé.");
            else if(result.success()) success.run();
            else status.accept(result.message());
        }));
    }
}
