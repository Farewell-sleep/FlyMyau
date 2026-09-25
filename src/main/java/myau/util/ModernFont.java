package myau.util;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Modern system-font renderer (TTF via AWT). Each character is rasterized
 * once with anti-aliasing into an RGBA texture and cached; rendering is a
 * simple textured quad per character. Falls back to a generic sans-serif
 * font when the requested family is unavailable.
 */
public class ModernFont {

    private final Font font;
    private final float ascent;
    private final int height;
    private final int spaceAdvance;
    private final Map<Character, Glyph> glyphs = new HashMap<>();

    private static final class Glyph {
        int texId;
        int width;
        int height;
        int advance;
    }

    public ModernFont(String family, int size) {
        Font f = new Font(family, Font.PLAIN, size);
        if (!f.canDisplay('A')) {
            f = new Font(Font.SANS_SERIF, Font.PLAIN, size);
        }
        this.font = f;
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        FontMetrics fm = probe.createGraphics().getFontMetrics(f);
        this.ascent = fm.getAscent();
        this.height = fm.getHeight();
        this.spaceAdvance = Math.max(1, fm.charWidth(' '));
    }

    public int getHeight() {
        return height;
    }

    public int getStringWidth(String text) {
        if (text == null || text.isEmpty()) return 0;
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            w += c == ' ' ? spaceAdvance : glyph(c).advance;
        }
        return w;
    }

    public void drawString(String text, float x, float y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) return;
        if (shadow) {
            drawText(text, x + 1.0F, y + 1.0F, 0xFF000000);
        }
        drawText(text, x, y, color);
    }

    private void drawText(String text, float x, float y, int color) {
        float a = ((color >> 24) & 255) / 255.0F;
        float r = ((color >> 16) & 255) / 255.0F;
        float g = ((color >> 8) & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(r, g, b, a);
        float cx = x;
        float top = y;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ') {
                cx += spaceAdvance;
                continue;
            }
            Glyph gl = glyph(c);
            if (gl.texId == 0) continue;
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, gl.texId);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 0);
            GL11.glVertex2f(cx, top);
            GL11.glTexCoord2f(1, 0);
            GL11.glVertex2f(cx + gl.width, top);
            GL11.glTexCoord2f(1, 1);
            GL11.glVertex2f(cx + gl.width, top + gl.height);
            GL11.glTexCoord2f(0, 1);
            GL11.glVertex2f(cx, top + gl.height);
            GL11.glEnd();
            cx += gl.advance;
        }
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableDepth();
    }

    private Glyph glyph(char c) {
        Glyph g = glyphs.get(c);
        if (g == null) {
            g = createGlyph(c);
            glyphs.put(c, g);
        }
        return g;
    }

    private Glyph createGlyph(char c) {
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        FontMetrics fm = probe.createGraphics().getFontMetrics(font);
        int w = Math.max(1, fm.charWidth(c));
        int h = Math.max(1, fm.getHeight());
        int advance = Math.max(1, fm.charWidth(c));

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setFont(font);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setColor(Color.WHITE);
        g2.drawString(String.valueOf(c), 0, fm.getAscent());
        g2.dispose();

        int texId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        ByteBuffer data = ByteBuffer.allocateDirect(w * h * 4);
        for (int i = 0; i < px.length; i++) {
            int argb = px[i];
            data.put((byte) ((argb >> 16) & 255));
            data.put((byte) ((argb >> 8) & 255));
            data.put((byte) (argb & 255));
            data.put((byte) ((argb >> 24) & 255));
        }
        data.flip();
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, data);

        Glyph g = new Glyph();
        g.texId = texId;
        g.width = w;
        g.height = h;
        g.advance = advance;
        return g;
    }
}
