package net.mebahel.zombiehorde.client;

import com.google.gson.*;
import net.mebahel.zombiehorde.config.HordeTemplates;
import net.minecraft.client.gui.screen.*;
import java.util.*;
import java.util.function.Consumer;

/** A task-oriented creator: starting model, composition/equipment, habitat, final review. */
final class HordeCreatorScreen extends HordeFormScreen {
    private final Screen parent;
    private final Consumer<JsonObject> apply;
    private JsonObject horde;
    private final JsonObject initial;
    private int step,mobIndex=-1;
    private String equipment="general";
    private boolean advanced;
    private final boolean isNew;
    HordeCreatorScreen(Screen parent,JsonObject existing,Consumer<JsonObject> apply){
        super(existing==null?"Créer une horde":"Modifier une horde");this.parent=parent;this.apply=apply;isNew=existing==null;
        horde=existing==null?HordeTemplates.create("zombies"):existing.deepCopy();initial=horde.deepCopy();
    }
    private JsonArray mobs(){return horde.getAsJsonArray("mobTypes");}
    private void changeStep(int target){if(checkInputs())resetScroll(()->{step=target;mobIndex=-1;});}
    @Override void tabs(){
        if(mobIndex>=0){
            String[] keys={"general","weapons","head","chest","legs","feet"},titles={"Créature","Armes","Casque","Plastron","Jambières","Bottes"};
            for(int i=0;i<keys.length;i++){String key=keys[i];tab(titles[i],i,keys.length,equipment.equals(key),()->{if(checkInputs()){equipment=key;rebuild();}});}
        }else{
            String[] names={"1 · Départ","2 · Créatures","3 · Lieux","4 · Résumé"};
            for(int i=0;i<names.length;i++){int target=i;tab(names[i],i,names.length,step==i,()->changeStep(target));}
        }
    }
    @Override void content(){
        if(mobIndex>=0){equipment();return;}
        switch(step){case 0->general();case 1->composition();case 2->places();default->review();}
    }
    private void general(){
        info("Une horde prête en quatre étapes","Commencez simplement : les réglages proposés sont déjà utilisables.");
        text(horde,"name","Nom de la horde","Un nom pour la retrouver dans votre liste ; 128 caractères maximum.");
        if(isNew){
            info("Choisir un point de départ","Un modèle remplace le contenu de cette nouvelle horde.");
            preset("zombies","Zombies classiques","Des zombies sans arme ; toutes les régions du monde normal.");
            preset("desert","Errants du désert","Des zombies momifiés ; uniquement dans les déserts.");
            preset("elite","Garde équipée","Des zombies portant une épée en fer ; monde normal.");
        }
        action("Réglages avancés","Facultatif : ajustez les poids pour rendre une horde plus rare.",advanced?"Masquer":"Afficher",()->{advanced=!advanced;rebuild();});
        if(advanced)number(horde,"weight","Fréquence relative","1 = normale ; 2 = deux fois plus fréquente qu’une horde de poids 1.",1,1000000,false);
    }
    private void preset(String model,String name,String hint){action(name,hint,"Utiliser ce modèle",()->client.setScreen(new ConfirmScreen(yes->{if(yes){horde=HordeTemplates.create(model);invalid.clear();status="Modèle prêt. Cliquez sur Suivant pour choisir les créatures.";}client.setScreen(this);},TextHelper.text("Utiliser ce modèle ?"),TextHelper.text("Le contenu actuel de cette nouvelle horde sera remplacé."))));}
    private void composition(){
        info("Qui fait partie de cette horde ?","Chaque membre est choisi parmi ces créatures selon sa proportion.");
        action("Ajouter un type de créature","Choisissez son nom dans la liste ; aucun identifiant à saisir.","+ Choisir une créature",()->client.setScreen(new HordeRegistryPicker(this,"id",id->{mobs().add(HordeTemplates.creature(id));status="Créature ajoutée. Son équipement peut maintenant être personnalisé.";})));
        for(int i=0;i<mobs().size();i++){
            int index=i;JsonObject mob=mobs().get(i).getAsJsonObject();
            action(TextHelper.entity(mob.get("id").getAsString()),TextHelper.share(mob,mobs())+" des membres en moyenne · équipement modifiable","Configurer",()->resetScroll(()->{mobIndex=index;equipment="general";}));
            if(advanced)number(mob,"weight","Proportion de cette créature","1 = une part ; 3 = trois parts. Les proportions sont calculées automatiquement.",1,1000000,false);
        }
        info("Exemple","Deux créatures de même poids donnent environ 50 % de chaque type.");
        action("Proportions avancées","Inutile si vous voulez autant de chaque créature.",advanced?"Masquer":"Afficher",()->{advanced=!advanced;rebuild();});
    }
    private JsonObject mob(){return mobs().get(mobIndex).getAsJsonObject();}
    private void equipment(){
        JsonObject mob=mob();String name=TextHelper.entity(mob.get("id").getAsString());
        if(equipment.equals("general")){
            info(name,"Vous modifiez uniquement ce type de créature dans cette horde.");
            action("Changer la créature","Les réglages d’équipement et de proportion sont conservés.","Choisir…",()->client.setScreen(new HordeRegistryPicker(this,"id",id->mob.addProperty("id",id))));
            action("Armure","Automatique : conserve l’équipement prévu par le système du mod.",mob.has("armor")?"Personnalisée":"Automatique",()->{
                if(!mob.has("armor")){JsonObject armor=new JsonObject();for(String slot:List.of("head","chest","legs","feet"))armor.add(slot,new JsonArray());mob.add("armor",armor);rebuild();}
                else client.setScreen(new ConfirmScreen(yes->{if(yes){mob.remove("armor");mob.remove("spawnWithArmorProbability");}client.setScreen(this);},TextHelper.text("Rétablir l’armure automatique ?"),TextHelper.text("Les listes d’armures personnalisées de cette créature seront effacées.")));
            });
            if(mob.has("armor")){
                action("Chance de porter une armure","Automatique : évolue avec la difficulté. Fixe : pourcentage de votre choix.",mob.has("spawnWithArmorProbability")?"Pourcentage fixe":"Selon la difficulté",()->{if(mob.has("spawnWithArmorProbability"))mob.remove("spawnWithArmorProbability");else mob.addProperty("spawnWithArmorProbability",1);rebuild();});
                if(mob.has("spawnWithArmorProbability"))number(mob,"spawnWithArmorProbability","Chance d’armure (%)","De 0 à 100 ; s’applique séparément à chaque emplacement.",0,100,true);
            }
            action("Retirer cette créature","Conservez au moins un type de créature dans la horde.","Retirer",()->{
                if(mobs().size()==1){status="Ajoutez une autre créature avant de retirer la dernière.";return;}
                client.setScreen(new ConfirmScreen(yes->{if(yes){invalid.remove(mob);mobs().remove(mobIndex);mobIndex=-1;scroll=0;}client.setScreen(this);},TextHelper.text("Retirer cette créature ?"),TextHelper.text(name)));
            });return;
        }
        boolean weapon=equipment.equals("weapons");
        if(!weapon&&!mob.has("armor")){
            info("Armure automatique","Le mod gère actuellement l’armure de cette créature.");
            action("Personnaliser l’armure","Les emplacements vides resteront vides.","Personnaliser",()->{JsonObject armor=new JsonObject();for(String slot:List.of("head","chest","legs","feet"))armor.add(slot,new JsonArray());mob.add("armor",armor);rebuild();});return;
        }
        if(weapon){number(mob,"spawnWithWeaponProbability","Créatures équipées d’une arme (%)","0 = aucune ; 100 = toutes. Une arme sera tirée parmi la liste.",0,100,true);}
        else info("Armure personnalisée · "+name,"Un objet est choisi dans cette liste. Une liste vide laisse l’emplacement vide.");
        JsonObject container=weapon?mob:mob.getAsJsonObject("armor");
        if(!container.has(equipment))container.add(equipment,new JsonArray());
        JsonArray items=container.getAsJsonArray(equipment);
        action("Ajouter un objet",weapon?"Choisissez une arme dans le catalogue.":"Choisissez une pièce adaptée à cet emplacement.","+ Choisir un objet",()->client.setScreen(new HordeRegistryPicker(this,"itemId",equipment,id->{JsonObject item=new JsonObject();item.addProperty("itemId",id);item.addProperty("weight",1);items.add(item);})));
        if(items.isEmpty())info("Aucun objet choisi",weapon?"Ajoutez une arme pour que le pourcentage d’équipement ait un effet.":"Ce membre de la horde ne portera rien à cet emplacement.");
        for(int i=0;i<items.size();i++){
            int index=i;JsonObject item=items.get(i).getAsJsonObject();String id=item.get("itemId").getAsString();
            action(TextHelper.item(id),TextHelper.share(item,items)+" des choix dans cette liste","Retirer",TextHelper.icon(id),()->{items.remove(index);invalid.remove(item);rebuild();});
            if(advanced)number(item,"weight","Fréquence de cet objet","Même poids = même chance. Un poids double donne deux fois plus de chances.",1,1000000,false);
        }
        action("Répartition avancée des objets","Facultatif : privilégier certaines armes ou pièces d’armure.",advanced?"Masquer":"Afficher",()->{advanced=!advanced;rebuild();});
    }
    private void places(){
        info("Où cette horde peut-elle apparaître ?","Choisissez au moins un monde ; les biomes sont facultatifs.");
        placeList("dimensions","Mondes autorisés","+ Ajouter un monde");
        info("Biomes autorisés",horde.getAsJsonArray("biomes").isEmpty()?"Tous les biomes des mondes choisis sont autorisés.":"Seuls les biomes ci-dessous sont autorisés.");
        placeList("biomes","Biomes","+ Choisir un biome");
    }
    private void placeList(String key,String title,String add){
        JsonArray values=horde.getAsJsonArray(key);
        action(title,key.equals("dimensions")?"Monde normal, Nether, End ou mondes ajoutés par vos mods.":"Laissez la liste vide pour autoriser tous les biomes.",add,()->client.setScreen(new HordeRegistryPicker(this,key,id->{boolean exists=false;for(JsonElement v:values)if(v.getAsString().equals(id))exists=true;if(!exists)values.add(id);})));
        for(int i=0;i<values.size();i++){
            int index=i;String id=values.get(i).getAsString();
            action(TextHelper.place(id),id,"Retirer",()->{if(key.equals("dimensions")&&values.size()==1){status="Conservez au moins un monde autorisé.";return;}values.remove(index);rebuild();});
        }
    }
    private void review(){
        info(horde.has("name")?horde.get("name").getAsString():"Ma horde","Vérifiez ces choix. Vous pourrez toujours les modifier ensuite.");
        info("Créatures",mobs().size()+" type(s) de créature. Le nombre total de membres dépend du système du mod.");
        for(JsonElement e:mobs()){
            JsonObject m=e.getAsJsonObject();String name=TextHelper.entity(m.get("id").getAsString());
            info(name,TextHelper.share(m,mobs())+" des membres · arme : "+Math.round(m.get("spawnWithWeaponProbability").getAsDouble()*100)+" %");
            if(m.getAsJsonArray("weapons").isEmpty()&&m.get("spawnWithWeaponProbability").getAsDouble()>0)info("À vérifier : "+name,"Chance d’arme définie, mais aucune arme n’a été choisie.");
        }
        for(JsonElement d:horde.getAsJsonArray("dimensions"))info("Monde autorisé",TextHelper.place(d.getAsString()));
        info("Biomes",horde.getAsJsonArray("biomes").isEmpty()?"Tous les biomes des mondes autorisés.":horde.getAsJsonArray("biomes").size()+" biome(s) sélectionné(s).");
        info("Dernière étape","Validez cette horde, puis enregistrez le brouillon dans la liste des hordes.");
    }
    @Override void footerButtons(){
        if(mobIndex>=0){footer("Retour aux créatures",0,1,()->{if(checkInputs())resetScroll(()->mobIndex=-1);},true);return;}
        footer("Annuler",0,3,this::close,true);
        footer("Précédent",1,3,()->changeStep(step-1),step>0);
        footer(step==3?"Valider cette horde":"Suivant",2,3,()->{if(step<3)changeStep(step+1);else finish();},true);
    }
    private void finish(){
        if(!checkInputs())return;
        if(mobs().isEmpty()||horde.getAsJsonArray("dimensions").isEmpty()){status="Choisissez au moins une créature et un monde.";return;}
        if(!horde.has("name")||horde.get("name").getAsString().isBlank())horde.addProperty("name","Ma horde");
        apply.accept(horde.deepCopy());client.setScreen(parent);
    }
    @Override public void close(){
        if(!horde.equals(initial)||!validInputs())client.setScreen(new ConfirmScreen(yes->client.setScreen(yes?parent:this),TextHelper.text("Abandonner cette modification ?"),TextHelper.text("La horde restera telle qu’elle était avant l’ouverture du créateur.")));
        else client.setScreen(parent);
    }
}
