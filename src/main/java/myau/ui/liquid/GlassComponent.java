package myau.ui.liquid;

/** Base class for all Liquid Glass GUI widgets. */
public abstract class GlassComponent {
    protected int x, y, w, h;

    public void setBounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getHeight() {
        return h;
    }

    public boolean isVisible() {
        return true;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    public abstract void draw(int mouseX, int mouseY);

    public void mouseDown(int mouseX, int mouseY, int button) {
    }

    public void mouseReleased(int mouseX, int mouseY, int button) {
    }

    public void keyTyped(char typedChar, int keyCode) {
    }
}
