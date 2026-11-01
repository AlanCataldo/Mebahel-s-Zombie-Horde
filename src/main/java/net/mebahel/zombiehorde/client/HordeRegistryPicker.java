package net.mebahel.zombiehorde.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
import java.util.*;
import java.util.function.Consumer;

/** Named, searchable catalogues: IDs are supporting information, never required input. */
final class HordeRegistryPicker extends HordeFormScreen {
    private final Screen parent;
    private final String kind,slot;
    private final Consumer<String> select;
    private final List<Entry> entries=new ArrayList<>();
    private String query="";
    private int cursor;
    private boolean recommended=true,loaded;
    private record Entry(String id,String name,ItemStack icon,boolean recommended){}
    HordeRegistryPicker(Screen parent,String kind,Consumer<String> select){this(parent,kind,"",select);}
    HordeRegistryPicker(Screen parent,String kind,String slot,Consumer<String> select){
        super(switch(kind){case "id"->"Choisir une créature";case "itemId"->"Choisir un équipement";case "biomes"->"Choisir un biome";default->"Choisir un monde";});
        this.parent=parent;this.kind=kind;this.slot=slot;this.select=select;
    }
    @Override protected void init(){if(!loaded){load();loaded=true;}rebuild();}
    private void load(){
        if(client.world==null)return;
        if(kind.equals("id")){
            for(Identifier id:Registries.ENTITY_TYPE.getIds()){
                var type=Registries.ENTITY_TYPE.get(id);
                try{if(!(type.create(client.world) instanceof MobEntity))continue;}catch(RuntimeException ignored){continue;}
                SpawnEggItem egg=SpawnEggItem.forEntity(type);
                entries.add(new Entry(id.toString(),type.getName().getString(),egg==null?ItemStack.EMPTY:new ItemStack(egg),true));
            }
        }else if(kind.equals("itemId")){
            for(Identifier id:Registries.ITEM.getIds()){
                Item item=Registries.ITEM.get(id);if(item==Items.AIR)continue;
                boolean match=switch(slot){
                    case "head","chest","legs","feet"->item instanceof ArmorItem armor&&armor.getSlotType().getName().equals(slot);
                    case "weapons"->item instanceof SwordItem||item instanceof AxeItem||item instanceof BowItem||item instanceof CrossbowItem||item instanceof TridentItem||item instanceof MaceItem;
                    default->true;
                };
                entries.add(new Entry(id.toString(),item.getName().getString(),new ItemStack(item),match));
            }
        }else{
            var ids=kind.equals("biomes")?client.world.getRegistryManager().get(RegistryKeys.BIOME).getIds():client.getNetworkHandler().getWorldKeys().stream().map(RegistryKey::getValue).collect(java.util.stream.Collectors.toSet());
            for(Identifier id:ids)entries.add(new Entry(id.toString(),TextHelper.place(id.toString()),ItemStack.EMPTY,true));
        }
        entries.sort(Comparator.comparing(Entry::name,String.CASE_INSENSITIVE_ORDER).thenComparing(Entry::id));
    }
    @Override void tabs(){
        TextFieldWidget search=addDrawableChild(new TextFieldWidget(textRenderer,left,51,panel,20,TextHelper.text("Rechercher par nom")));fixed.add(search);
        search.setMaxLength(128);search.setPlaceholder(TextHelper.text("Rechercher par nom ou par mod…"));search.setText(query);
        setInitialFocus(search);search.setCursor(Math.min(cursor,query.length()),false);
        search.setChangedListener(value->{
            cursor=search.getCursor();
            // Yarn also notifies this listener when only the caret/selection changes.
            if(value.equals(query))return;
            query=value;scroll=0;rebuild();
        });
    }
    @Override void content(){
        String needle=query.toLowerCase(Locale.ROOT);
        List<Entry> visible=entries.stream().filter(e->(!kind.equals("itemId")||!recommended||e.recommended)&&(e.name.toLowerCase(Locale.ROOT).contains(needle)||e.id.toLowerCase(Locale.ROOT).contains(needle))).toList();
        info(visible.size()+" résultat(s)",kind.equals("id")?"Seules les créatures compatibles avec les hordes sont proposées.":"Cliquez sur Choisir ; aucun identifiant à recopier.");
        for(Entry e:visible)action(e.name,e.id,"Choisir",e.icon,()->{select.accept(e.id);client.setScreen(parent);});
        if(visible.isEmpty())info("Aucun résultat","Essayez un nom plus court ou affichez tous les objets.");
    }
    @Override void footerButtons(){
        if(kind.equals("itemId")){
            footer(recommended?"Afficher tous les objets":"Objets recommandés",0,2,()->resetScroll(()->recommended=!recommended),true);
            footer("Annuler",1,2,this::close,true);
        }else footer("Annuler",0,1,this::close,true);
    }
    @Override public void close(){client.setScreen(parent);}
}
