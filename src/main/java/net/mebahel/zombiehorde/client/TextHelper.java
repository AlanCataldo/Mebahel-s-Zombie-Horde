package net.mebahel.zombiehorde.client;

import com.google.gson.*;
import net.minecraft.text.Text;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.item.ItemStack;

final class TextHelper {
    static Text text(String s){return Text.literal(s);}
    static String entity(String id) {var key=Identifier.tryParse(id);return key!=null&&Registries.ENTITY_TYPE.containsId(key)?Registries.ENTITY_TYPE.get(key).getName().getString():id;}
    static String item(String id) {var key=Identifier.tryParse(id);return key!=null&&Registries.ITEM.containsId(key)?Registries.ITEM.get(key).getName().getString():id;}
    static ItemStack icon(String id) {var key=Identifier.tryParse(id);return key!=null&&Registries.ITEM.containsId(key)?new ItemStack(Registries.ITEM.get(key)):ItemStack.EMPTY;}
    static String name(JsonObject h,int index){return h.has("name")&&!h.get("name").getAsString().isBlank()?h.get("name").getAsString():"Horde "+(index+1);}
    static String share(JsonObject o,JsonArray list) {long total=0;for(JsonElement e:list)total+=e.getAsJsonObject().get("weight").getAsLong();return String.format(java.util.Locale.FRANCE,"%.1f %%",total==0?0:o.get("weight").getAsDouble()*100/total);}
    static String place(String id) {return switch(id){case "minecraft:overworld"->"Monde normal";case "minecraft:the_nether"->"Nether";case "minecraft:the_end"->"End";default->Text.translatable("biome."+id.replace(':','.')).getString();};}
}
