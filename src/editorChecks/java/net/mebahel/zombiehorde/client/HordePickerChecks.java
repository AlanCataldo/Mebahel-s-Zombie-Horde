package net.mebahel.zombiehorde.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;

/** Exercises the real caret callbacks without bootstrapping a world or renderer. */
public final class HordePickerChecks {
    public static void run() throws Exception {
        for(String kind:java.util.List.of("id","itemId","biomes","dimensions")) {
            HordeRegistryPicker picker=new HordeRegistryPicker(null,kind,id->{});
            var renderer=Screen.class.getDeclaredField("textRenderer");renderer.setAccessible(true);
            renderer.set(picker,new TextRenderer(id->null,false) {
                @Override public String trimToWidth(String s,int width){return s.substring(0,Math.min(s.length(),Math.max(0,width/6)));}
                @Override public String trimToWidth(String s,int width,boolean backwards){return trimToWidth(s,width);}
            });
            var query=HordeRegistryPicker.class.getDeclaredField("query");query.setAccessible(true);
            picker.panel=600;picker.left=20;
            for(String text:java.util.List.of("","zombie","")) {
                query.set(picker,text);picker.fixed.clear();picker.tabs();
                TextFieldWidget field=search(picker);
                if(!field.getText().equals(text))throw new AssertionError("Search text lost during restoration");
                for(int i=0;i<100;i++) {
                    field.setCursor(i%(text.length()+1),false);
                    field.setSelectionEnd((i+1)%(text.length()+1));
                    field.setText(text);
                    if(search(picker)!=field)throw new AssertionError("Unchanged text rebuilt the picker");
                }
            }
        }
        System.out.println("Picker checks passed: empty/populated search restoration and repeated caret/selection changes in all four catalogues.");
    }
    private static TextFieldWidget search(HordeRegistryPicker picker) {
        return (TextFieldWidget)picker.fixed.stream().filter(TextFieldWidget.class::isInstance).findFirst().orElseThrow();
    }
}
