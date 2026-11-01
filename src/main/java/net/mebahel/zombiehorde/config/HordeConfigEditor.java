package net.mebahel.zombiehorde.config;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import net.fabricmc.loader.api.FabricLoader;
import net.mebahel.zombiehorde.MebahelZombieHorde;
import net.mebahelsapi.config.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.server.MinecraftServer;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Independent, validated drafts; the existing JSON files remain authoritative. */
public final class HordeConfigEditor implements MebahelConfigHandler<String> {
    public static final Codec<String> CODEC = Codec.STRING;
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static String serverSnapshot;
    public static JsonObject snapshot() {
        JsonObject root = new JsonObject(), settings = new JsonObject();
        for (Field f : fields()) try { settings.add(f.getName(), GSON.toJsonTree(f.get(null))); }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }
        root.add("settings", settings);
        root.add("hordes", GSON.toJsonTree(HordeMemberModConfig.hordeCompositions));
        for (JsonElement element : root.getAsJsonArray("hordes")) {
            JsonObject h = element.getAsJsonObject();
            if (!h.has("biomes")) h.add("biomes",new JsonArray());
            for (JsonElement member : h.getAsJsonArray("mobTypes")) {
                JsonObject mob = member.getAsJsonObject();
                if (!mob.has("weapons")) mob.add("weapons",new JsonArray());
                for (JsonElement weapon : mob.getAsJsonArray("weapons")) {
                    JsonObject w = weapon.getAsJsonObject();
                    if (!w.has("weight") || w.get("weight").getAsInt() <= 0) {
                        int weight = w.has("chance") ? Math.max(1, Math.round(w.get("chance").getAsFloat()*100)) : 1;
                        w.addProperty("weight",weight);
                    }
                    w.remove("chance");
                }
                if (mob.has("armor")) {
                    JsonObject armor = mob.getAsJsonObject("armor");
                    for (String slot : List.of("head","chest","legs","feet")) if (!armor.has(slot)) armor.add(slot,new JsonArray());
                }
            }
        }
        return root;
    }
    private static List<Field> fields() {
        return Arrays.stream(ZombieHordeModConfig.class.getFields())
            .filter(f -> Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers())).toList();
    }
    @Override public String current() { return GSON.toJson(snapshot()); }
    @Override public ValidationResult validate(String json) {
        try {
            JsonObject root=JsonParser.parseString(json).getAsJsonObject(); check(root); checkRegistered(root);
            if(HordeConfigSync.server != null) {
                MinecraftServer server=HordeConfigSync.server;
                Set<String> dimensions=new HashSet<>();server.getWorldRegistryKeys().forEach(k -> dimensions.add(k.getValue().toString()));
                var biomes=server.getRegistryManager().get(net.minecraft.registry.RegistryKeys.BIOME);
                Set<String> checked=new HashSet<>();
                for(JsonElement e:root.getAsJsonArray("hordes")) {
                    JsonObject h=e.getAsJsonObject();
                    for(JsonElement d:h.getAsJsonArray("dimensions")) require(dimensions.contains(d.getAsString()),"Dimension inconnue : "+d.getAsString());
                    if(h.has("biomes")) for(JsonElement b:h.getAsJsonArray("biomes")) require(biomes.containsId(Identifier.of(b.getAsString())),"Biome inconnu : "+b.getAsString());
                    for(JsonElement m:h.getAsJsonArray("mobTypes")) {
                        String id=m.getAsJsonObject().get("id").getAsString();
                        if(checked.add(id)) {
                            var entity=Registries.ENTITY_TYPE.get(Identifier.of(id)).create(server.getOverworld());
                            require(entity instanceof net.minecraft.entity.mob.MobEntity,"Cette entité n’est pas une créature : "+id);
                        }
                    }
                }
            }
            return ValidationResult.success();
        }
        catch (RuntimeException e) { return ValidationResult.error(e.getMessage()); }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalArgumentException(message); }
    private static void checkRegistered(JsonElement element) {
        if(element.isJsonArray()) {for(JsonElement child:element.getAsJsonArray()) checkRegistered(child);}
        else if(element.isJsonObject()) {
            JsonObject o=element.getAsJsonObject();
            if(o.has("id")) require(Registries.ENTITY_TYPE.containsId(Identifier.of(o.get("id").getAsString())),"Créature inconnue : "+o.get("id").getAsString());
            if(o.has("itemId")) require(Registries.ITEM.containsId(Identifier.of(o.get("itemId").getAsString())),"Objet inconnu : "+o.get("itemId").getAsString());
            for(JsonElement child:o.asMap().values()) checkRegistered(child);
        }
    }
    private static double number(JsonObject o, String key, double min, double max) {
        JsonPrimitive p = o.getAsJsonPrimitive(key);
        require(p != null && p.isNumber(), key + ": nombre requis");
        double n = p.getAsDouble();
        require(Double.isFinite(n) && n >= min && n <= max, key + " : valeur entre " + min + " et " + max);
        return n;
    }
    private static void integer(JsonObject o, String key, int min, int max) {
        double n = number(o, key, min, max); require(n == Math.rint(n), key + " : entier requis");
    }
    private static void id(String s) { require(Identifier.tryParse(s) != null, "Identifiant invalide : " + s); }
    public static void check(JsonObject root) {
        require(root.keySet().equals(Set.of("settings", "hordes")), "Configuration incomplète");
        JsonObject s = root.getAsJsonObject("settings");
        require(s.size() == fields().size(), "Paramètres incomplets");
        for (Field f : fields()) {
            JsonPrimitive p = s.getAsJsonPrimitive(f.getName());
            require(p != null, "Paramètre manquant : " + f.getName());
            if (f.getType() == boolean.class) require(p.isBoolean(), f.getName() + " : booléen requis");
        }
        integer(s,"hordeSpawnDelayAmount",1,1000000);
        require(Set.of("minute","day").contains(s.get("hordeSpawnDelayUnit").getAsString()), "Unité : minute ou day");
        number(s,"hordeSpawnChance",0,1); integer(s,"randomNumberHordeReinforcements",0,10);
        integer(s,"hordeNumber",1,20); integer(s,"hordeMemberBonusHealth",0,800);
        integer(s,"hordeSpawnDistanceFromPlayer",1,512);
        JsonArray hordes = root.getAsJsonArray("hordes");
        require(!hordes.isEmpty() && hordes.size() <= 256, "Il faut entre 1 et 256 hordes");
        long total = 0;
        for (JsonElement element : hordes) {
            JsonObject h = element.getAsJsonObject(); integer(h,"weight",1,1000000); total += h.get("weight").getAsInt();
            if (h.has("name")) require(h.get("name").getAsString().length() <= 128,"Le nom d’une horde est limité à 128 caractères");
            require(h.getAsJsonArray("dimensions") != null && !h.getAsJsonArray("dimensions").isEmpty(), "Une dimension est requise");
            for (String key : List.of("dimensions","biomes")) if (h.has(key)) for (JsonElement v : h.getAsJsonArray(key)) id(v.getAsString());
            JsonArray mobs = h.getAsJsonArray("mobTypes"); require(mobs != null && !mobs.isEmpty() && mobs.size() <= 128,"Il faut entre 1 et 128 créatures");
            long mobTotal = 0;
            for (JsonElement m : mobs) {
                JsonObject mob = m.getAsJsonObject(); String entity = mob.get("id").getAsString(); id(entity);
                integer(mob,"weight",1,1000000); mobTotal += mob.get("weight").getAsInt();
                number(mob,"spawnWithWeaponProbability",0,1);
                if (mob.has("spawnWithArmorProbability")) number(mob,"spawnWithArmorProbability",0,1);
                gear(mob.getAsJsonArray("weapons"));
                if (mob.has("armor")) for (String slot : List.of("head","chest","legs","feet")) gear(mob.getAsJsonObject("armor").getAsJsonArray(slot));
            }
            require(mobTotal <= Integer.MAX_VALUE, "Somme des poids des créatures trop élevée");
        }
        require(total <= Integer.MAX_VALUE,"Somme des poids des hordes trop élevée");
    }
    private static void gear(JsonArray list) {
        if (list == null) return;
        require(list.size() <= 128,"128 équipements maximum par liste");
        for (JsonElement e : list) {
            JsonObject g = e.getAsJsonObject(); String item = g.get("itemId").getAsString(); id(item);
            integer(g,"weight",1,1000000);
        }
    }
    @Override public void apply(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        for (Field f : fields()) try { f.set(null, GSON.fromJson(root.getAsJsonObject("settings").get(f.getName()), f.getType())); }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }
        HordeMemberModConfig.hordeCompositions = new ArrayList<>(Arrays.asList(GSON.fromJson(root.get("hordes"), HordeMemberModConfig.HordeComposition[].class)));
    }
    @Override public void save() throws Exception {
        Path DIRECTORY = FabricLoader.getInstance().getConfigDir().resolve(MebahelZombieHorde.MOD_ID);
        Files.createDirectories(DIRECTORY);
        // Stage both files before replacing either, retaining originals if the second move fails.
        Path settings = DIRECTORY.resolve(MebahelZombieHorde.MOD_ID + "_config.json");
        Path hordes = DIRECTORY.resolve(MebahelZombieHorde.MOD_ID + "mob_type_config.json");
        JsonObject s = snapshot().getAsJsonObject("settings");
        s.add("spawnInDayLight",s.remove("spawnInDaylight")); s.add("hordeSpawning",s.remove("patrolSpawning"));
        JsonArray delay = new JsonArray(); delay.add(s.remove("hordeSpawnDelayAmount")); delay.add(s.remove("hordeSpawnDelayUnit")); s.add("hordeSpawnDelay",delay);
        JsonObject h = new JsonObject(); h.add("hordeCompositions",snapshot().get("hordes"));
        byte[] oldSettings = Files.exists(settings) ? Files.readAllBytes(settings) : null;
        Path a = Files.createTempFile(DIRECTORY,"settings-",".tmp"), b = Files.createTempFile(DIRECTORY,"hordes-",".tmp");
        try {
            Files.writeString(a,GSON.toJson(s)); Files.writeString(b,GSON.toJson(h));
            Files.move(a,settings,StandardCopyOption.REPLACE_EXISTING);
            try { Files.move(b,hordes,StandardCopyOption.REPLACE_EXISTING); }
            catch (Exception e) { if (oldSettings != null) Files.write(settings,oldSettings); else Files.deleteIfExists(settings); throw e; }
        } finally { Files.deleteIfExists(a); Files.deleteIfExists(b); }
    }
    @Override public void reload(MinecraftServer server) { HordeConfigSync.syncAll(server); }
}
