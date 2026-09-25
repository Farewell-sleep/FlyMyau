package myau.ui;

import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.ui.liquid.GlassRenderer;
import myau.util.ModernFont;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Modern toast notifications for module toggles — Rise-style cards that
 * slide in from the right edge, show the module name with an ON/OFF state
 * (green/red accent bar + label) and a lifetime progress line, then fade
 * out. Rendered on Render2DEvent; always active (client core layer).
 */
public final class ToastManager {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ToastManager INSTANCE = new ToastManager();
    private static final ModernFont FONT = new ModernFont("Segoe UI", 16);

    public static final int ON_COLOR = 0xFF22C55E;
    public static final int OFF_COLOR = 0xFFEF4444;
    private static final int TEXT_MAIN = 0xFFF2F4F8;

    private static final long SLIDE_MS = 220L;
    private static final long SHOW_MS = 1500L;
    private static final long FADE_MS = 420L;
    private static final int HEIGHT = 30;
    private static final int GAP = 6;
    private static final int RIGHT_PAD = 6;
    private static final int TOP_PAD = 6;
    private static final int MAX_TOASTS = 5;

    private static final class Toast {
        final String name;
        final boolean enabled;
        final long created;
        final int width;

        Toast(String name, boolean enabled) {
            this.name = name;
            this.enabled = enabled;
            this.created = System.currentTimeMillis();
            int nameW = FONT.getStringWidth(name);
            int stateW = FONT.getStringWidth(enabled ? "ON" : "OFF");
            this.width = Math.max(160, nameW + stateW + 64);
        }
    }

    private final List<Toast> toasts = new CopyOnWriteArrayList<>();

    private ToastManager() {
    }

    /** Called once at startup (ModuleToggleNotify.init). */
    public static void register() {
        myau.event.EventManager.register(INSTANCE);
    }

    public static void push(String moduleName, boolean enabled) {
        if (moduleName == null || moduleName.isEmpty()) return;
        INSTANCE.toasts.add(new Toast(moduleName, enabled));
        while (INSTANCE.toasts.size() > MAX_TOASTS) {
            INSTANCE.toasts.remove(0);
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (toasts.isEmpty()) return;
        long now = System.currentTimeMillis();
        ScaledResolution sr = new ScaledResolution(mc);
        int y = TOP_PAD;
        Iterator<Toast> it = toasts.iterator();
        while (it.hasNext()) {
            Toast t = it.next();
            long age = now - t.created;
            if (age > SHOW_MS + FADE_MS) {
                it.remove();
                continue;
            }
            int x = sr.getScaledWidth() - t.width - RIGHT_PAD;
            float alpha = 1.0F;
            if (age < SLIDE_MS) {
                float p = age / (float) SLIDE_MS;
                x += (int) ((1.0F - p) * 36.0F);
                alpha = p;
            } else if (age > SHOW_MS) {
                alpha = 1.0F - (age - SHOW_MS) / (float) FADE_MS;
            }
            if (alpha > 0.0F) {
                drawToast(x, y, t, alpha);
            }
            y += HEIGHT + GAP;
        }
    }

    private static void drawToast(int x, int y, Toast t, float alpha) {
        int stateColor = t.enabled ? ON_COLOR : OFF_COLOR;
        GlassRenderer.drawRoundedGradient(x, y, t.width, HEIGHT, 6,
                argb(0xF0141B28, alpha), argb(0xE60D1016, alpha));
        GlassRenderer.drawRoundedOutline(x + 0.5F, y + 0.5F, t.width - 1, HEIGHT - 1, 6, 1.0F, argb(0x26FFFFFF, alpha));
        // accent bar (left)
        GlassRenderer.drawRoundedRect(x + 4, y + 5, 3, HEIGHT - 10, 1.5F, argb(stateColor, alpha));
        // module name
        FONT.drawString(t.name, x + 14, y + 6, argb(TEXT_MAIN, alpha), true);
        // state label (right)
        String state = t.enabled ? "ON" : "OFF";
        int stateW = FONT.getStringWidth(state);
        FONT.drawString(state, x + t.width - stateW - 12, y + 6, argb(stateColor, alpha), true);
        // lifetime progress line
        long age = System.currentTimeMillis() - t.created;
        if (age < SHOW_MS) {
            float prog = 1.0F - age / (float) SHOW_MS;
            float w = (t.width - 16) * prog;
            if (w > 0.0F) {
                GlassRenderer.drawRect(x + 8, y + HEIGHT - 4, w, 1.5F, argb(stateColor, alpha * 0.8F));
            }
        }
    }

    private static int argb(int rgb, float alpha) {
        int a = (int) (Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F);
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
