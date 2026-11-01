package net.mebahel.zombiehorde.config;

import com.google.gson.*;

/** Executable regression checks; no test framework or additional dependency required. */
public final class HordeConfigEditorChecks {
    public static void main(String[] args) throws Exception {
        net.mebahel.zombiehorde.client.HordePickerChecks.run();
        JsonObject initial=HordeConfigEditor.snapshot();
        HordeConfigEditor.check(initial);
        String before=HordeConfigEditor.GSON.toJson(initial);
        JsonObject draft=initial.deepCopy();
        JsonObject mob=draft.getAsJsonArray("hordes").get(0).getAsJsonObject().getAsJsonArray("mobTypes").get(0).getAsJsonObject();
        mob.addProperty("spawnWithWeaponProbability",0);
        HordeConfigEditor.check(draft);
        if(!before.equals(HordeConfigEditor.GSON.toJson(HordeConfigEditor.snapshot()))) throw new AssertionError("Draft mutated live config");
        HordeConfigEditor editor=new HordeConfigEditor();
        editor.apply(HordeConfigEditor.GSON.toJson(draft));
        if(HordeMemberModConfig.hordeCompositions.getFirst().mobTypes.getFirst().spawnWithWeaponProbability!=0) throw new AssertionError("Zero chance lost");
        editor.apply(before);
        for(String model : java.util.List.of("zombies","desert","elite")) {
            JsonObject candidate=initial.deepCopy();
            JsonArray hordes=new JsonArray();hordes.add(HordeTemplates.create(model));candidate.add("hordes",hordes);
            HordeConfigEditor.check(candidate);
            editor.apply(HordeConfigEditor.GSON.toJson(candidate));
            String name=candidate.getAsJsonArray("hordes").get(0).getAsJsonObject().get("name").getAsString();
            if(!name.equals(HordeMemberModConfig.hordeCompositions.getFirst().name)) throw new AssertionError("Horde name lost");
            if(HordeMemberModConfig.hordeCompositions.getFirst().mobTypes.getFirst().armor!=null) throw new AssertionError("Automatic armor was replaced");
            if(!HordeConfigEditor.snapshot().getAsJsonArray("hordes").get(0).getAsJsonObject().get("name").getAsString().equals(name)) throw new AssertionError("Name missing from server snapshot");
        }
        editor.apply(before);
        JsonObject first=HordeTemplates.create("elite"),second=HordeTemplates.create("elite");
        first.getAsJsonArray("mobTypes").get(0).getAsJsonObject().getAsJsonArray("weapons").get(0).getAsJsonObject().addProperty("weight",99);
        if(second.getAsJsonArray("mobTypes").get(0).getAsJsonObject().getAsJsonArray("weapons").get(0).getAsJsonObject().get("weight").getAsInt()!=1) throw new AssertionError("Starting models share mutable equipment");
        JsonObject invalid=initial.deepCopy();invalid.getAsJsonObject("settings").addProperty("hordeNumber",1.5);
        rejects(invalid);
        invalid=initial.deepCopy();invalid.getAsJsonArray("hordes").get(0).getAsJsonObject().addProperty("weight",0);rejects(invalid);
        invalid=initial.deepCopy();invalid.add("hordes",new JsonArray());rejects(invalid);
        invalid=initial.deepCopy();invalid.getAsJsonObject("settings").addProperty("hordeSpawnChance",1.1);rejects(invalid);
        invalid=initial.deepCopy();invalid.getAsJsonArray("hordes").get(0).getAsJsonObject().getAsJsonArray("mobTypes").get(0).getAsJsonObject().addProperty("id","!invalid");rejects(invalid);
        invalid=initial.deepCopy();invalid.getAsJsonArray("hordes").get(0).getAsJsonObject().addProperty("name","x".repeat(129));rejects(invalid);
        System.out.println("Horde editor checks passed: presets, independent equipment, persistent names, automatic armor, draft isolation, apply/rollback, zero chance, invalid ranges and identifiers.");
    }
    private static void rejects(JsonObject value) {
        try {HordeConfigEditor.check(value);} catch(RuntimeException expected) {return;}
        throw new AssertionError("Invalid config accepted: "+value);
    }
}
