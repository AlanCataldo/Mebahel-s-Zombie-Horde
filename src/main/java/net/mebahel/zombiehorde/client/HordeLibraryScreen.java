package net.mebahel.zombiehorde.client;

import com.google.gson.*;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.item.ItemStack;

final class HordeLibraryScreen extends HordeFormScreen {
    private final Screen parent;
    private final HordeEditorDraft draft;
    private final Runnable saved;
    HordeLibraryScreen(Screen parent,HordeEditorDraft draft,Runnable saved){super("Créateur de hordes — Mes hordes");this.parent=parent;this.draft=draft;this.saved=saved;}
    private JsonArray hordes(){return draft.root.getAsJsonArray("hordes");}
    @Override boolean busy(){return draft.saving;}
    @Override void tabs(){tab("Mes hordes",0,2,true,()->{});tab("+ Nouvelle horde",1,2,false,()->client.setScreen(new HordeCreatorScreen(this,null,h->{hordes().add(h);status="Horde ajoutée au brouillon. Enregistrez pour l’activer.";})));}
    @Override void content(){
        info("Créez ou choisissez une horde","Les changements restent dans un brouillon jusqu’à « Enregistrer ».");
        for(int i=0;i<hordes().size();i++){
            int index=i;JsonObject h=hordes().get(i).getAsJsonObject();String name=TextHelper.name(h,i);
            String members=h.getAsJsonArray("mobTypes").size()+" type(s) de créature · fréquence relative : "+TextHelper.share(h,hordes());
            form.add(new FormRow(name,members,ItemStack.EMPTY,(x,y,w)->{
                int copy=w<150?22:48,remove=22,edit=w-copy-remove-8;
                controls.add(button(w<150?"Voir":"Modifier",x,y,edit,()->client.setScreen(new HordeCreatorScreen(this,h,edited->{hordes().set(index,edited);status="Horde modifiée dans le brouillon. Pensez à enregistrer.";}))));
                var duplicate=button(w<150?"+":"Copier",x+edit+4,y,copy,()->{JsonObject c=h.deepCopy();c.addProperty("name",name.substring(0,Math.min(119,name.length()))+" (copie)");hordes().add(c);status="Copie ajoutée au brouillon.";rebuild();});
                duplicate.setTooltip(Tooltip.of(TextHelper.text("Créer une copie indépendante de « "+name+" »")));controls.add(duplicate);
                var delete=button("×",x+w-remove,y,remove,()->client.setScreen(new ConfirmScreen(yes->{if(yes)hordes().remove(index);client.setScreen(this);},TextHelper.text("Supprimer « "+name+" » ?"),TextHelper.text("La suppression prendra effet après l’enregistrement du brouillon."))));
                delete.active=hordes().size()>1;delete.setTooltip(Tooltip.of(TextHelper.text(hordes().size()>1?"Supprimer cette horde":"Conservez au moins une horde.")));controls.add(delete);
            }));
        }
        if(hordes().isEmpty())info("Aucune horde","Ajoutez une horde avant d’enregistrer.");
    }
    @Override void footerButtons(){footer("Retour à la configuration",0,2,this::close,!draft.saving);footer(draft.saving?"Enregistrement…":"Enregistrer le brouillon",1,2,()->draft.save(s->{status=s;rebuild();},saved),draft.canSave()&&!hordes().isEmpty());}
    @Override public void close(){if(!draft.saving)client.setScreen(parent);}
}
