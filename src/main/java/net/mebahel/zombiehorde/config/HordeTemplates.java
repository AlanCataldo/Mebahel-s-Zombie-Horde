package net.mebahel.zombiehorde.config;

import com.google.gson.*;

/** Safe, editable starting points; no registry IDs need to be typed by the player. */
public final class HordeTemplates {
    private HordeTemplates() {}
    public static JsonObject creature(String id) {
        JsonObject mob=new JsonObject();mob.addProperty("id",id);mob.addProperty("weight",1);
        mob.addProperty("spawnWithWeaponProbability",0);mob.add("weapons",new JsonArray());
        return mob;
    }
    public static JsonObject create(String model) {
        JsonObject h=new JsonObject();h.addProperty("name",switch(model){case "desert"->"Les errants du désert";case "elite"->"La garde déchue";default->"Ma horde de zombies";});
        h.addProperty("weight",1);JsonArray dimensions=new JsonArray();dimensions.add("minecraft:overworld");h.add("dimensions",dimensions);
        JsonArray biomes=new JsonArray();if(model.equals("desert"))biomes.add("minecraft:desert");h.add("biomes",biomes);
        JsonObject mob=creature(model.equals("desert")?"minecraft:husk":"minecraft:zombie");
        if(model.equals("elite")) {
            mob.addProperty("spawnWithWeaponProbability",1);JsonObject weapon=new JsonObject();weapon.addProperty("itemId","minecraft:iron_sword");weapon.addProperty("weight",1);mob.getAsJsonArray("weapons").add(weapon);
        }
        JsonArray mobs=new JsonArray();mobs.add(mob);h.add("mobTypes",mobs);return h;
    }
}
