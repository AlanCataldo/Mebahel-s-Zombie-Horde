package net.mebahel.zombiehorde.client;

import com.google.gson.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.*;

/** QuestConfigScreen's geometry, palette, alternating rows, fixed footer and pixel scrolling. */
abstract class HordeFormScreen extends Screen {
    static final int ROW=42, TOP=85;
    int left,panel,listHeight,footer,scroll;
    String status="";
    final CreatorScrollbar scrollbar=new CreatorScrollbar();
    final List<ClickableWidget> fixed=new ArrayList<>(), controls=new ArrayList<>();
    final List<FormRow> form=new ArrayList<>();
    final IdentityHashMap<JsonObject,Map<String,String>> invalid=new IdentityHashMap<>();
    private ButtonWidget selectedTab;
    record FormRow(String label,String hint,ItemStack icon,Control control) {}
    interface Control {void build(int x,int y,int width);}
    HordeFormScreen(String title) {super(Text.literal(title));}
    abstract void content();
    abstract void tabs();
    abstract void footerButtons();
    boolean busy() {return false;}
    @Override protected void init() {rebuild();}
    final void rebuild() {
        clearChildren();fixed.clear();controls.clear();form.clear();selectedTab=null;
        panel=Math.min(650,width-24);left=(width-panel)/2;
        content();
        int available=Math.max(ROW,(height-TOP-55)/ROW*ROW);
        listHeight=Math.min(available,Math.max(ROW,form.size()*ROW));footer=TOP+listHeight+12;
        scroll=Math.max(0,Math.min(scroll,form.size()*ROW-listHeight));
        tabs();
        scrollbar.configure(left+panel-8,TOP,listHeight,form.size()*ROW,scroll);
        int controlW=Math.max(90,Math.min(178,panel/3));
        for(int i=scroll/ROW;i<Math.min(form.size(),(scroll+listHeight)/ROW+2);i++)
            form.get(i).control.build(left+panel-controlW-20,TOP+i*ROW-scroll+10,controlW);
        footerButtons();
        if(busy()) {fixed.forEach(w->w.active=false);controls.forEach(w->w.active=false);}
    }
    void tab(String text,int index,int total,boolean selected,Runnable action) {
        int gap=4,w=(panel-gap*(total-1))/total;
        ButtonWidget b=button(text,left+index*(w+gap),51,w,() -> {scroll=0;action.run();});fixed.add(b);
        if(selected) selectedTab=b;
    }
    ButtonWidget button(String text,int x,int y,int w,Runnable action) {
        return addDrawableChild(ButtonWidget.builder(Text.literal(text),b -> action.run()).dimensions(x,y,w,20).build());
    }
    void footer(String text,int index,int total,Runnable action,boolean enabled) {
        int gap=8,w=Math.min(170,(panel-16-gap*(total-1))/total),start=left+(panel-total*w-gap*(total-1))/2;
        ButtonWidget b=button(text,start+index*(w+gap),footer,w,action);b.active=enabled;fixed.add(b);
    }
    void info(String title,String hint) {form.add(new FormRow(title,hint,ItemStack.EMPTY,(x,y,w)->{}));}
    void action(String title,String hint,String caption,Runnable callback) {action(title,hint,caption,ItemStack.EMPTY,callback);}
    void action(String title,String hint,String caption,ItemStack icon,Runnable callback) {
        form.add(new FormRow(title,hint,icon,(x,y,w)->{
            ButtonWidget b=button(caption,x,y,w,callback);
            b.setTooltip(Tooltip.of(Text.literal(title+"\n"+hint)));controls.add(b);
        }));
    }
    void toggle(JsonObject o,String key,String title,String hint) {
        form.add(new FormRow(title,hint,ItemStack.EMPTY,(x,y,w) -> controls.add(button(o.get(key).getAsBoolean()?"Activé":"Désactivé",x,y,w,() -> {o.addProperty(key,!o.get(key).getAsBoolean());rebuild();}))));
    }
    void text(JsonObject o,String key,String title,String hint) {field(o,key,title,hint,false,false,0,0);}
    void number(JsonObject o,String key,String title,String hint,double min,double max,boolean percent) {field(o,key,title,hint,true,percent,min,max);}
    private void field(JsonObject o,String key,String title,String hint,boolean numeric,boolean percent,double min,double max) {
        form.add(new FormRow(title,hint,ItemStack.EMPTY,(x,y,w) -> {
            TextFieldWidget f=addDrawableChild(new TextFieldWidget(textRenderer,x,y,w,20,Text.literal(title)));controls.add(f);
            f.setMaxLength(numeric?16:128);
            String value=o.has(key)?o.get(key).getAsString():"";
            if(percent) value=new BigDecimal(value).multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString();
            Map<String,String> errors=invalid.computeIfAbsent(o,k->new HashMap<>());
            f.setText(errors.getOrDefault(key,value));f.setEditableColor(errors.containsKey(key)?0xFF7777:0xFFFFFF);
            f.setTooltip(Tooltip.of(Text.literal(title+"\n"+hint)));
            f.setChangedListener(s -> {
                try {
                    if(numeric) {
                        BigDecimal n=new BigDecimal(s.replace(',','.'));
                        double v=n.doubleValue();
                        if(!Double.isFinite(v)||v<min||v>max||(!percent && v!=Math.rint(v))) throw new NumberFormatException();
                        o.add(key,new JsonPrimitive(percent?n.divide(BigDecimal.valueOf(100)):n));
                    } else o.addProperty(key,s);
                    errors.remove(key);f.setEditableColor(0xFFFFFF);
                } catch(RuntimeException e) {errors.put(key,s);f.setEditableColor(0xFF7777);}
            });
        }));
    }
    boolean validInputs() {return invalid.values().stream().allMatch(Map::isEmpty);}
    boolean checkInputs() {if(validInputs()) return true;status="Corrigez les champs rouges : utilisez une valeur dans la plage indiquée.";return false;}
    void resetScroll(Runnable change) {scroll=0;change.run();rebuild();}
    @Override public boolean mouseScrolled(double x,double y,double h,double v) {if(y<TOP||y>=TOP+listHeight)return super.mouseScrolled(x,y,h,v);int next=scrollbar.scroll(v);if(next!=scroll){scroll=next;rebuild();}return true;}
    @Override public boolean mouseClicked(double x,double y,int b) {
        if(b==0&&scrollbar.click(x,y)){scroll=scrollbar.value();rebuild();return true;}
        if(y>=TOP&&y<TOP+listHeight)return super.mouseClicked(x,y,b);
        for(ClickableWidget widget:fixed)if(widget.mouseClicked(x,y,b))return true;return false;
    }
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy) {if(b==0&&scrollbar.drag(y)){scroll=scrollbar.value();rebuild();return true;}return super.mouseDragged(x,y,b,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int b) {if(b==0&&scrollbar.release())return true;return super.mouseReleased(x,y,b);}
    @Override public void render(DrawContext c,int mx,int my,float delta) {
        renderBackground(c,mx,my,delta);
        c.fill(left-8,38,left+panel+8,footer+29,0xD918202A);
        c.drawCenteredTextWithShadow(textRenderer,title,width/2,13,0xFFFFFFFF);
        for(ClickableWidget widget:fixed)widget.render(c,mx,my,delta);
        if(selectedTab!=null)c.fill(selectedTab.getX()+2,selectedTab.getY()+18,selectedTab.getX()+selectedTab.getWidth()-2,selectedTab.getY()+20,0xFFE0E0E0);
        c.enableScissor(left,TOP,left+panel-10,TOP+listHeight);
        int controlW=Math.max(90,Math.min(178,panel/3)),labelW=panel-controlW-35;
        for(int i=scroll/ROW;i<Math.min(form.size(),(scroll+listHeight)/ROW+2);i++) {
            FormRow row=form.get(i);int y=TOP+i*ROW-scroll;
            c.fill(left+3,y+2,left+panel-12,y+ROW-3,i%2==0?0x77304455:0x77273340);
            int icon=row.icon.isEmpty()?0:24;
            if(icon>0)c.drawItem(row.icon,left+10,y+12);
            c.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(row.label,labelW-icon),left+12+icon,y+9,0xFFFFFFFF);
            c.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(row.hint,labelW-icon),left+12+icon,y+23,0xFFAFC0CC);
        }
        for(ClickableWidget widget:controls)widget.render(c,mx,my,delta);
        c.disableScissor();scrollbar.render(c,mx,my);
        if(!status.isBlank())c.drawCenteredTextWithShadow(textRenderer,textRenderer.trimToWidth(status,panel),width/2,TOP+listHeight+2,0xFFFFD687);
        if(mx>=left&&mx<left+panel-controlW-24&&my>=TOP&&my<TOP+listHeight){
            int hovered=(my-TOP+scroll)/ROW;
            if(hovered<form.size()){
                FormRow row=form.get(hovered);
                c.drawOrderedTooltip(textRenderer,textRenderer.wrapLines(Text.literal(row.label+"\n"+row.hint),Math.min(320,width-30)),mx,my);
            }
        }
    }
}
