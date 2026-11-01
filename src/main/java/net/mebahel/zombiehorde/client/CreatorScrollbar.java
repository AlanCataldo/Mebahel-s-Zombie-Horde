package net.mebahel.zombiehorde.client;

import net.minecraft.client.gui.DrawContext;

/** Small draggable scrollbar. Pixel mode supports smooth creator pickers. */
final class CreatorScrollbar {
    private int x, top, height, max, value;
    private int unit = 1;
    private boolean dragging;
    private int dragOffset;

    void configure(int x, int top, int height, int contentHeight, int value) {
        this.x = x;
        this.top = top;
        this.height = Math.max(1, height);
        this.max = Math.max(0, contentHeight - height);
        this.unit = 1;
        this.value = Math.max(0, Math.min(this.max, value));
    }

    void configure(int x, int top, int height, int totalRows, int visibleRows, int value) {
        this.x = x;
        this.top = top;
        this.height = Math.max(1, height);
        this.max = Math.max(0, totalRows - visibleRows);
        this.unit = Math.max(1, height / Math.max(1, visibleRows));
        this.value = Math.max(0, Math.min(this.max, value));
    }

    int value() { return value; }

    int scroll(double amount) {
        value = Math.max(0, Math.min(max, value - (int) Math.round(amount * 20 / unit)));
        return value;
    }

    boolean click(double mouseX, double mouseY) {
        if (max == 0 || mouseX < x || mouseX >= x + 8 || mouseY < top || mouseY >= top + height) return false;
        int thumbTop = thumbTop();
        if (mouseY >= thumbTop && mouseY < thumbTop + thumbHeight()) {
            dragging = true;
            dragOffset = (int) mouseY - thumbTop;
        } else {
            value = Math.max(0, Math.min(max, value + (mouseY < thumbTop ? -height / unit : height / unit)));
        }
        return true;
    }

    boolean drag(double mouseY) {
        if (!dragging) return false;
        int travel = height - thumbHeight();
        if (travel > 0) value = Math.max(0, Math.min(max,
                (int) Math.round((mouseY - dragOffset - top) * max / travel)));
        return true;
    }

    boolean release() {
        boolean wasDragging = dragging;
        dragging = false;
        return wasDragging;
    }

    void render(DrawContext context, int mouseX, int mouseY) {
        if (max == 0) return;
        boolean hovered = mouseX >= x && mouseX < x + 8 && mouseY >= top && mouseY < top + height;
        renderStyled(context, x, top, height, thumbTop(), thumbHeight(), 8, hovered, dragging);
    }

    static void renderStyled(DrawContext context, int x, int top, int height,
                             int thumbTop, int thumbHeight, int width, boolean hovered, boolean dragging) {
        int center = x + width / 2;
        context.fill(center - 3, top, center + 3, top + height, 0xCC121820);
        int left = center - 3;
        int border = dragging ? 0xFFF2F2F2 : hovered ? 0xFFE3E3E3 : 0xFFBDBDBD;
        int face = dragging ? 0xFFB5B5B5 : hovered ? 0xFF9F9F9F : 0xFF858585;
        context.fill(left, thumbTop, left + 6, thumbTop + thumbHeight, border);
        if (thumbHeight > 2) context.fill(left + 1, thumbTop + 1, left + 5, thumbTop + thumbHeight - 1, face);
    }

    private int thumbHeight() {
        return Math.min(height, 28);
    }

    private int thumbTop() {
        return top + (height - thumbHeight()) * value / Math.max(1, max);
    }
}
