package net.mebahel.zombiehorde.client;

import com.google.gson.*;
import net.minecraft.client.gui.screen.*;

/** Same layout and sections as QuestConfigScreen, with Horde-specific explanatory copy. */
public final class HordeConfigScreen extends HordeFormScreen {
    private final Screen parent;
    private final HordeEditorDraft draft=new HordeEditorDraft();
    private String section="Apparition";
    public HordeConfigScreen(Screen parent) {super("Mebahel's Zombie Horde — Configuration");this.parent=parent;}
    @Override boolean busy(){return draft.saving;}
    @Override void tabs() {
        String[] names={"Apparition","Difficulté","Comportement"};
        for(int i=0;i<names.length;i++){String name=names[i];tab(name,i,names.length,section.equals(name),()->{section=name;rebuild();});}
    }
    @Override void content() {
        JsonObject s=draft.root.getAsJsonObject("settings");
        switch(section) {
            case "Apparition" -> {
                toggle(s,"patrolSpawning","Faire apparaître des hordes","Désactivez pour arrêter les apparitions automatiques.");
                toggle(s,"spawnInDaylight","Autoriser les apparitions de jour","Désactivé : les hordes apparaissent uniquement la nuit.");
                number(s,"hordeSpawnDelayAmount","Temps entre deux tentatives","De 1 à 1 000 000 ; choisissez l’unité sur la ligne suivante.",1,1000000,false);
                action("Unité du délai","Une minute réelle ou un jour Minecraft (20 minutes).",s.get("hordeSpawnDelayUnit").getAsString().equals("day")?"Jours Minecraft":"Minutes",()->{s.addProperty("hordeSpawnDelayUnit",s.get("hordeSpawnDelayUnit").getAsString().equals("day")?"minute":"day");rebuild();});
                number(s,"hordeSpawnChance","Réussite d’une tentative (%)","De 0 à 100 ; 100 signifie une horde à chaque tentative admissible.",0,100,true);
                number(s,"hordeSpawnDistanceFromPlayer","Distance du joueur (blocs)","De 1 à 512 blocs ; 60 est le réglage par défaut.",1,512,false);
                toggle(s,"showHordeSpawningMessage","Annoncer l’arrivée d’une horde","Affiche un message quand une horde apparaît.");
            }
            case "Difficulté" -> {
                toggle(s,"enableDifficultySystem","Augmenter la difficulté au fil du jeu","Le système du mod fait évoluer la menace avec la progression.");
                number(s,"hordeNumber","Nombre de hordes","De 1 à 20 ; augmente le nombre de hordes générées.",1,20,false);
                number(s,"randomNumberHordeReinforcements","Renforts aléatoires","De 0 à 10 ; 0 désactive les renforts supplémentaires.",0,10,false);
                number(s,"hordeMemberBonusHealth","Santé supplémentaire par créature","De 0 à 800 points ; 2 points correspondent à un cœur.",0,800,false);
            }
            default -> {
                toggle(s,"hordeMemberBreakGlass","Autoriser la destruction des vitres","Les créatures des hordes peuvent traverser les vitres.");
                toggle(s,"hordeMemberBreakFence","Autoriser la destruction des barrières","Les créatures des hordes peuvent ouvrir un passage.");
                info("Vous cherchez la composition des hordes ?","Utilisez « Créer et modifier les hordes » ci-dessous.");
            }
        }
    }
    @Override void footerButtons() {
        footer("Créer et modifier les hordes",0,3,()->{if(checkInputs())client.setScreen(new HordeLibraryScreen(this,draft,()->client.setScreen(parent)));},!draft.saving);
        footer("Annuler",1,3,this::close,!draft.saving);
        footer(draft.saving?"Enregistrement…":"Enregistrer",2,3,()->{if(checkInputs())draft.save(s->{status=s;rebuild();},()->client.setScreen(parent));},draft.canSave());
        if(draft.original==null)status="Ouvrez un monde pour modifier sa configuration.";
    }
    @Override public void close() {
        if(draft.saving)return;
        if(draft.dirty()||!validInputs())client.setScreen(new ConfirmScreen(yes->client.setScreen(yes?parent:this),TextHelper.text("Abandonner les modifications ?"),TextHelper.text("Les changements non enregistrés seront perdus.")));
        else client.setScreen(parent);
    }
}
