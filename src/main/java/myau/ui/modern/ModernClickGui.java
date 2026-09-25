package myau.ui.modern;

import myau.OpenMyau;
import myau.module.Module;
import myau.module.modules.*;
import myau.ui.liquid.GlassComponent;
import myau.ui.liquid.GlassControls;
import myau.ui.liquid.GlassRenderer;
import myau.util.ModernFont;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Modern ClickGUI — dark flat design with accent highlights.
 *
 * Layout: top bar (brand + style switch + search), left category rail,
 * module card grid, inline property panel per card. Reuses GlassRenderer
 * drawing primitives and GlassControls property widgets.
 */
public class ModernClickGui extends GuiScreen {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // palette
    public static final int BG_TOP = 0xFF0D1016;
    public static final int BG_BOTTOM = 0xFF141B28;
    public static final int CARD_BODY = 0xE0151C28;
    public static final int CARD_BODY_HOVER = 0xF01E2737;
    public static final int CARD_ACTIVE = 0xE0172420;
    public static final int ACCENT = 0xFF22C55E;
    public static final int ACCENT_DIM = 0x4022C55E;
    public static final int BORDER = 0x26FFFFFF;
    public static final int BORDER_HOVER = 0x6622C55E;
    public static final int TEXT_MAIN = 0xFFF2F4F8;
    public static final int TEXT_DIM = 0xFF97A0B4;
    public static final int TEXT_FAINT = 0xFF5A6276;
    public static final int DIVIDER = 0x1AFFFFFF;

    private static final int TOP_H = 42;
    private static final int RAIL_X = 16;
    private static final int RAIL_W = 102;
    private static final int RAIL_BTN_H = 30;
    private static final int RAIL_GAP = 8;
    private static final int CARD_W = 134;
    private static final int CARD_H = 58;
    private static final int CARD_GAP = 12;
    private static final int CONTENT_TOP = 54;

    // modern font (system Segoe UI, used for all text in this UI)
    private final ModernFont uiFont = new ModernFont("Segoe UI", 16);

    // state
    private final List<Category> categories = new ArrayList<>();
    private int selected;
    private final StringBuilder search = new StringBuilder();
    private boolean searchFocused;
    private int scroll;
    private float animScroll;
    private boolean dragging;
    private int dragOffX, dragOffY;
    private int offsetX, offsetY;   // whole-window drag offset

    public ModernClickGui() {
        categories.add(new Category("Combat", AimAssist.class, AutoClicker.class, KillAura.class, Wtap.class,
                Velocity.class, Freeze.class, Reach.class, TargetStrafe.class, NoHitDelay.class, AntiFireball.class,
                LagRange.class, BackTrack.class, BlockHit.class, AutoBlock.class, HitBox.class, MoreKB.class, Refill.class, HitSelect.class));
        categories.add(new Category("Movement", AntiAFK.class, Fly.class, Speed.class, LongJump.class, Sprint.class,
                SafeWalk.class, Jesus.class, Blink.class, NoFall.class, NoSlow.class, KeepSprint.class, Eagle.class,
                NoJumpDelay.class, AntiVoid.class));
        categories.add(new Category("Render", ESP.class, Chams.class, FullBright.class, Tracers.class, NameTags.class,
                Xray.class, TargetHUD.class, Indicators.class, BedESP.class, ItemESP.class, ItemPhysics.class, BreakProgress.class,
                Freelook.class, ViewClip.class, NoHurtCam.class,
                HUD.class, GuiModule.class, ChestESP.class, Trajectories.class, Radar.class, CuteVisuals.class, SnowFog.class));
        categories.add(new Category("Player", AutoHeal.class, AutoTool.class, ChestStealer.class, InvManager.class,
                InvWalk.class, Scaffold.class, NewScaffold.class, Telly.class, AutoBlockIn.class, SpeedMine.class, FastPlace.class,
                GhostHand.class, MCF.class, AntiDebuff.class));
        categories.add(new Category("Misc", Spammer.class, BedNuker.class, BedTracker.class, LightningTracker.class,
                NoRotate.class, NickHider.class, AntiObbyTrap.class, AntiObfuscate.class, AutoAnduril.class,
                InventoryClicker.class));
    }

    // ------------------------------------------------------------------
    // category data
    // ------------------------------------------------------------------

    private static final class Category {
        final String name;
        final List<Module> modules = new ArrayList<>();

        Category(String name, Class<?>... classes) {
            this.name = name;
            for (Class<?> c : classes) {
                Module m = OpenMyau.moduleManager.getModule(c);
                if (m != null) modules.add(m);
            }
            modules.sort(Comparator.comparing(m -> m.getName().toLowerCase()));
        }
    }

    private List<Module> visibleModules() {
        String q = search.toString().trim().toLowerCase();
        if (!q.isEmpty()) {
            List<Module> out = new ArrayList<>();
            for (Category cat : categories) {
                for (Module m : cat.modules) {
                    if (m.getName().toLowerCase().contains(q)) out.add(m);
                }
            }
            return out;
        }
        return categories.get(selected).modules;
    }

    // ------------------------------------------------------------------
    // GuiScreen lifecycle
    // ------------------------------------------------------------------

    @Override
    public void initGui() {
        super.initGui();
        offsetX = offsetY = 0;
        GlassControls.fontOverride = uiFont;
    }

    @Override
    public void onGuiClosed() {
        GlassControls.fontOverride = null;
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawBackground(mouseX, mouseY);

        int ox = offsetX, oy = offsetY;

        // top bar
        GlassRenderer.drawRoundedGradient(0 + ox, 0 + oy, width, TOP_H, 0, 0x99000000, 0x00000000);
        GlassRenderer.drawRect(0 + ox, TOP_H + oy - 1, width, 1, DIVIDER);
        uiFont.drawString("OpenMyau++", 18 + ox, 12 + oy, ACCENT, true);
        uiFont.drawString("Modern UI", 18 + ox, 33 + oy, TEXT_FAINT, true);

        // style switch (LiquidGlass | Modern)
        int swX = width - 168 + ox;
        boolean hoverLiquid = isIn(swX, 12 + oy, 58, 18, mouseX, mouseY);
        boolean hoverModern = isIn(swX + 62, 12 + oy, 58, 18, mouseX, mouseY);
        GlassRenderer.drawCapsuleRect(swX, 12 + oy, 58, 18, hoverLiquid ? 0x22FFFFFF : 0x14FFFFFF);
        uiFont.drawString("LiquidGlass", swX + 8, 11 + oy, hoverLiquid ? TEXT_MAIN : TEXT_DIM, false);
        GlassRenderer.drawCapsuleRect(swX + 62, 12 + oy, 58, 18, ACCENT);
        uiFont.drawString("Modern", swX + 70, 11 + oy, 0xFF06110A, false);

        // search
        int searchX = swX - 230;
        int searchW = 220;
        GlassRenderer.drawCapsuleRect(searchX, 12 + oy, searchW, 18, searchFocused ? 0x2EFFFFFF : 0x14FFFFFF);
        GlassRenderer.drawCapsuleOutline(searchX + 0.5F, 12.5F + oy, searchW - 1, 17, 1.0F, 0x22FFFFFF);
        String hint = search.length() == 0 ? "Search modules..." : search.toString();
        uiFont.drawString(hint, searchX + 10, 11 + oy, search.length() == 0 ? TEXT_FAINT : TEXT_MAIN, false);
        if (searchFocused) {
            GlassRenderer.drawRect(searchX + 12 + uiFont.getStringWidth(hint), 15 + oy, 1.2F, 10, ACCENT);
        }

        // left category rail
        int railY = CONTENT_TOP + oy;
        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            int by = railY + i * (RAIL_BTN_H + RAIL_GAP);
            boolean sel = i == selected;
            boolean hover = isIn(RAIL_X + ox, by, RAIL_W, RAIL_BTN_H, mouseX, mouseY);
            int body = sel ? ACCENT : hover ? 0x22FFFFFF : 0x10FFFFFF;
            GlassRenderer.drawRoundedRect(RAIL_X + ox, by, RAIL_W, RAIL_BTN_H, 8, body);
            if (sel) {
                GlassRenderer.drawRoundedRect(RAIL_X + ox, by + 6, 3, RAIL_BTN_H - 12, 1.5F, 0xFF0B150F);
            } else {
                GlassRenderer.drawRoundedOutline(RAIL_X + 0.5F + ox, by + 0.5F, RAIL_W - 1, RAIL_BTN_H - 1, 8, 1.0F, 0x14FFFFFF);
            }
            int count = search.length() == 0 ? cat.modules.size() : -1;
            uiFont.drawString(cat.name, RAIL_X + 12 + ox, by + 6, sel ? 0xFF0B150F : TEXT_MAIN, false);
            if (count >= 0) {
                uiFont.drawString(String.valueOf(count), RAIL_X + RAIL_W - 10 - uiFont.getStringWidth(String.valueOf(count)) + ox, by + 6, sel ? 0x990B150F : TEXT_FAINT, false);
            }
        }

        // module grid
        drawModuleGrid(mouseX, mouseY, ox, oy);

        // scrollbar
        List<Module> mods = visibleModules();
        int contentW = width - (RAIL_X + RAIL_W + 16) - 18;
        int cols = Math.max(1, (contentW + CARD_GAP) / (CARD_W + CARD_GAP));
        int rows = (int) Math.ceil(mods.size() / (float) cols);
        int totalH = rows * (CARD_H + CARD_GAP) + 8;
        int viewH = height - CONTENT_TOP - 14;
        if (totalH > viewH) {
            int maxScroll = totalH - viewH;
            if (scroll > maxScroll) scroll = maxScroll;
            if (scroll < 0) scroll = 0;
            float thumbH = Math.max(20, viewH * viewH / (float) totalH);
            float thumbY = CONTENT_TOP + oy + (viewH - thumbH) * (scroll / (float) maxScroll);
            GlassRenderer.drawRoundedRect(width - 5 + ox, thumbY, 2, thumbH, 1, 0x5522C55E);
        }

        // debug / brand footer
        uiFont.drawString("Rise-style Modern", 18 + ox, height - 21 + oy, TEXT_FAINT, true);
    }

    // ------------------------------------------------------------------
    // module grid
    // ------------------------------------------------------------------

    private void drawModuleGrid(int mouseX, int mouseY, int ox, int oy) {
        List<Module> mods = visibleModules();
        int contentX = RAIL_X + RAIL_W + 14 + ox;
        int contentW = width - (RAIL_X + RAIL_W + 16) - 18;
        int cols = Math.max(1, (contentW + CARD_GAP) / (CARD_W + CARD_GAP));
        int rows = (int) Math.ceil(mods.size() / (float) cols);
        int totalH = rows * (CARD_H + CARD_GAP) + 8;
        int viewH = height - CONTENT_TOP - 14;
        int maxScroll = Math.max(0, totalH - viewH);
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;
        animScroll += (scroll - animScroll) * 0.2F;
        if (Math.abs(scroll - animScroll) < 0.1F) animScroll = scroll;

        // grid y positions with per-row expansion offsets
        int[] rowY = new int[rows];
        int[] rowExtra = new int[rows];
        int yPos = CONTENT_TOP + oy - (int) animScroll;
        for (int r = 0; r < rows; r++) {
            rowY[r] = yPos;
            int maxExtra = 0;
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                if (idx >= mods.size()) break;
                Module mod = mods.get(idx);
                ModernCard card = cardFor(mod);
                maxExtra = Math.max(maxExtra, (int) card.expandH());
            }
            rowExtra[r] = maxExtra;
            yPos += CARD_H + CARD_GAP + maxExtra;
        }

        // draw cards (scissored to content area)
        int bottom = CONTENT_TOP + oy + viewH;
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(mc);
        double scaleF = sr.getScaleFactor();
        int scX = (int) ((contentX - 2) * scaleF);
        int scY = (int) ((sr.getScaledHeight() - bottom) * scaleF);
        int scW = (int) ((contentW + 2) * scaleF);
        int scH = (int) ((viewH + 2) * scaleF);
        if (scH > 0) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(scX, scY, scW, scH);
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                if (idx >= mods.size()) break;
                Module mod = mods.get(idx);
                int cx = contentX + c * (CARD_W + CARD_GAP);
                ModernCard card = cardFor(mod);
                card.draw(cx, rowY[r], CARD_W, mouseX, mouseY);
            }
        }
        if (scH > 0) GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    /** Expandable module card. */
    private final class ModernCard {
        final Module module;
        final List<GlassComponent> controls;
        boolean expanded;
        private boolean lastHovered;
        private long hoverStart;
        private long openStart;
        private boolean pressed;
        private long pressStart;
        private long releaseStart;

        ModernCard(Module module) {
            this.module = module;
            this.controls = GlassControls.buildFor(module, CARD_W - 16);
            long now = System.currentTimeMillis();
            this.hoverStart = now;
            this.openStart = now - GlassRenderer.DURATION;
            this.pressStart = now;
            this.releaseStart = now;
        }

        float expandH() {
            if (!expanded) return 0;
            int h = 8;
            for (GlassComponent c : controls) {
                if (c.isVisible()) h += c.getHeight() + 5;
            }
            return h;
        }

        boolean hovered(int mouseX, int mouseY, int cx, int cy) {
            return mouseX >= cx && mouseX <= cx + CARD_W
                    && mouseY >= cy && mouseY <= cy + CARD_H + expandH();
        }

        void draw(int cx, int cy, int w, int mouseX, int mouseY) {
            long now = System.currentTimeMillis();
            boolean hover = hovered(mouseX, mouseY, cx, cy);
            if (hover != lastHovered) {
                hoverStart = now;
                lastHovered = hover;
            }
            float hoverEase = GlassRenderer.spring(GlassRenderer.easeOutCubic(GlassRenderer.animate(hoverStart, now)));
            float pressEase;
            if (pressed) {
                pressEase = GlassRenderer.spring(GlassRenderer.easeOutCubic(GlassRenderer.animate(pressStart, now)));
            } else {
                pressEase = 1 - GlassRenderer.spring(GlassRenderer.easeOutCubic(GlassRenderer.animate(releaseStart, now)));
            }
            float expand = expanded
                    ? GlassRenderer.spring(GlassRenderer.easeOutCubic(GlassRenderer.animate(openStart, now)))
                    : 1 - GlassRenderer.spring(GlassRenderer.easeOutCubic(GlassRenderer.animate(openStart, now)));

            float lift = hoverEase * 2.0F - pressEase * 3.0F;
            float dy = cy - lift;
            boolean on = module.isEnabled();

            // shadow
            GlassRenderer.drawRoundedRect(cx, dy + 2, w, CARD_H, 10, 0x22000000);
            GlassRenderer.drawRoundedRect(cx, dy + 4, w, CARD_H, 10, 0x14000000);
            // body
            int body = on ? CARD_ACTIVE : (hover ? CARD_BODY_HOVER : CARD_BODY);
            GlassRenderer.drawRoundedRect(cx, dy, w, CARD_H, 10, body);
            // accent top strip when enabled
            if (on) {
                GlassRenderer.drawRoundedGradient(cx + 1, dy + 1, w - 2, 4, 2,
                        (0x66 << 24) | (ACCENT & 0xFFFFFF), ACCENT & 0xFFFFFF);
            }
            // border
            int border = on ? BORDER_HOVER : (hover ? 0x3AFFFFFF : BORDER);
            GlassRenderer.drawRoundedOutline(cx + 0.5F, dy + 0.5F, w - 1, CARD_H - 1, 10, 1.0F, border);

            // name
            int nameColor = on ? ACCENT : (hover ? TEXT_MAIN : TEXT_DIM);
            uiFont.drawString(module.getName(), cx + 11, (int) (dy + 12), nameColor, false);
            // state pill
            GlassRenderer.drawCapsule(cx + w - 32, dy + 10, 22, 11, on, ACCENT);
            // suffix / hint
            String[] suffix = module.getSuffix();
            String sub = (suffix != null && suffix.length > 0 && suffix[0] != null) ? suffix[0]
                    : (module.getKey() != 0 ? "Bind: " + org.lwjgl.input.Keyboard.getKeyName(module.getKey()) : "RMB: settings");
            uiFont.drawString(sub, cx + 11, (int) (dy + 38), TEXT_FAINT, false);

            // expansion panel
            float eh = expandH() * expand;
            if (eh > 0.5F) {
                int py = (int) (dy + CARD_H);
                int ph = (int) eh;
                GlassRenderer.drawRoundedRect(cx, py, w, ph, 8, 0xEE10151F);
                GlassRenderer.drawRoundedOutline(cx + 0.5F, py + 0.5F, w - 1, ph - 1, 8, 1.0F, 0x1AFFFFFF);
                int cy2 = py + 8;
                for (GlassComponent c : controls) {
                    if (!c.isVisible()) continue;
                    c.setBounds(cx + 8, cy2, w - 16, c.getHeight());
                    c.draw(mouseX, mouseY);
                    cy2 += c.getHeight() + 5;
                }
            }
        }

        void mouseDown(int mouseX, int mouseY, int cx, int cy, int button) {
            float eh = expandH();
            boolean inCard = mouseX >= cx && mouseX <= cx + CARD_W && mouseY >= cy && mouseY <= cy + CARD_H;
            boolean inPanel = eh > 0 && mouseX >= cx && mouseX <= cx + CARD_W
                    && mouseY >= cy + CARD_H && mouseY <= cy + CARD_H + eh;
            if (inCard) {
                pressed = true;
                pressStart = System.currentTimeMillis();
                if (button == 0) {
                    module.toggle();
                } else if (button == 1) {
                    expanded = !expanded;
                    openStart = System.currentTimeMillis();
                }
                return;
            }
            if (inPanel) {
                int py = cy + CARD_H;
                int cy2 = py + 8;
                for (GlassComponent c : controls) {
                    if (!c.isVisible()) continue;
                    int ch = c.getHeight();
                    if (mouseY >= cy2 && mouseY <= cy2 + ch) {
                        c.mouseDown(mouseX, mouseY, button);
                        return;
                    }
                    cy2 += ch + 5;
                }
            }
        }

        void mouseReleased(int mouseX, int mouseY, int cx, int cy, int button) {
            if (pressed) {
                pressed = false;
                releaseStart = System.currentTimeMillis();
            }
            float eh = expandH();
            if (eh > 0 && mouseX >= cx && mouseX <= cx + CARD_W
                    && mouseY >= cy + CARD_H && mouseY <= cy + CARD_H + eh) {
                int py = cy + CARD_H;
                int cy2 = py + 8;
                for (GlassComponent c : controls) {
                    if (!c.isVisible()) continue;
                    int ch = c.getHeight();
                    if (mouseY >= cy2 && mouseY <= cy2 + ch) {
                        c.mouseReleased(mouseX, mouseY, button);
                        return;
                    }
                    cy2 += ch + 5;
                }
            }
        }

        void keyTyped(char typedChar, int keyCode) {
            if (!expanded) return;
            for (GlassComponent c : controls) {
                if (c.isVisible()) c.keyTyped(typedChar, keyCode);
            }
        }
    }

    private final List<ModernCard> cards = new ArrayList<>();

    private ModernCard cardFor(Module m) {
        for (ModernCard c : cards) {
            if (c.module == m) return c;
        }
        ModernCard c = new ModernCard(m);
        cards.add(c);
        return c;
    }

    // ------------------------------------------------------------------
    // background
    // ------------------------------------------------------------------

    private void drawBackground(int mouseX, int mouseY) {
        GlassRenderer.drawRoundedGradient(0, 0, width, height, 0, BG_TOP, BG_BOTTOM);
        // soft accent glow blobs
        GlassRenderer.drawCircle(width * 0.12F, height * 0.18F, 190, 0x081D7C2E);
        GlassRenderer.drawCircle(width * 0.85F, height * 0.78F, 220, 0x0622C55E);
        GlassRenderer.drawCircle(width * 0.7F, height * 0.12F, 120, 0x0414B8A6);
        // subtle grid noise: horizontal hairlines
        for (int y = 60; y < height; y += 46) {
            GlassRenderer.drawRect(0, y, width, 1, 0x04000000);
        }
    }

    private static boolean isIn(float x, float y, float w, float h, int mx, int my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    // ------------------------------------------------------------------
    // input
    // ------------------------------------------------------------------

    @Override
    public void handleMouseInput() throws java.io.IOException {
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            List<Module> mods = visibleModules();
            int contentW = width - (RAIL_X + RAIL_W + 16) - 18;
            int cols = Math.max(1, (contentW + CARD_GAP) / (CARD_W + CARD_GAP));
            int rows = (int) Math.ceil(mods.size() / (float) cols);
            int totalH = rows * (CARD_H + CARD_GAP) + 8;
            int viewH = height - CONTENT_TOP - 14;
            int maxScroll = Math.max(0, totalH - viewH);
            int dir = wheel > 0 ? -1 : 1;
            scroll = Math.max(0, Math.min(maxScroll, scroll + dir * 28));
        }
        super.handleMouseInput();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        int ox = offsetX, oy = offsetY;

        // style switch
        int swX = width - 168 + ox;
        if (mouseButton == 0) {
            if (isIn(swX, 12 + oy, 58, 18, mouseX, mouseY)) {
                myau.module.modules.GuiModule gm = (myau.module.modules.GuiModule) OpenMyau.moduleManager.getModule(GuiModule.class);
                if (gm != null) gm.setStyle(0);
                mc.displayGuiScreen(new myau.ui.liquid.LiquidClickGui());
                return;
            }
            if (isIn(swX + 62, 12 + oy, 58, 18, mouseX, mouseY)) {
                return;
            }
        }

        // search focus
        int searchX = swX - 230;
        if (isIn(searchX, 12 + oy, 220, 18, mouseX, mouseY)) {
            searchFocused = true;
            return;
        }
        searchFocused = false;

        // drag top bar
        if (mouseButton == 0 && mouseY <= TOP_H) {
            dragging = true;
            dragOffX = mouseX - offsetX;
            dragOffY = mouseY - offsetY;
            return;
        }

        // category rail
        int railY = CONTENT_TOP + oy;
        for (int i = 0; i < categories.size(); i++) {
            int by = railY + i * (RAIL_BTN_H + RAIL_GAP);
            if (isIn(RAIL_X + ox, by, RAIL_W, RAIL_BTN_H, mouseX, mouseY)) {
                if (mouseButton == 0) {
                    selected = i;
                    search.setLength(0);
                    scroll = 0;
                    animScroll = 0;
                }
                return;
            }
        }

        // module cards
        List<Module> mods = visibleModules();
        int contentX = RAIL_X + RAIL_W + 14 + ox;
        int contentW = width - (RAIL_X + RAIL_W + 16) - 18;
        int cols = Math.max(1, (contentW + CARD_GAP) / (CARD_W + CARD_GAP));
        int rows = (int) Math.ceil(mods.size() / (float) cols);
        int[] rowY = new int[rows];
        int yPos = CONTENT_TOP + oy - (int) animScroll;
        for (int r = 0; r < rows; r++) {
            rowY[r] = yPos;
            int maxExtra = 0;
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                if (idx >= mods.size()) break;
                maxExtra = Math.max(maxExtra, (int) cardFor(mods.get(idx)).expandH());
            }
            yPos += CARD_H + CARD_GAP + maxExtra;
        }
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                if (idx >= mods.size()) break;
                ModernCard card = cardFor(mods.get(idx));
                int cx = contentX + c * (CARD_W + CARD_GAP);
                if (mouseX >= cx && mouseX <= cx + CARD_W
                        && mouseY >= rowY[r] && mouseY <= rowY[r] + CARD_H + card.expandH()) {
                    card.mouseDown(mouseX, mouseY, cx, rowY[r], mouseButton);
                    return;
                }
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        if (dragging) {
            dragging = false;
            return;
        }
        List<Module> mods = visibleModules();
        int contentX = RAIL_X + RAIL_W + 14 + offsetX;
        int contentW = width - (RAIL_X + RAIL_W + 16) - 18;
        int cols = Math.max(1, (contentW + CARD_GAP) / (CARD_W + CARD_GAP));
        int rows = (int) Math.ceil(mods.size() / (float) cols);
        int yPos = CONTENT_TOP + offsetY - (int) animScroll;
        for (int r = 0; r < rows; r++) {
            int rowY = yPos;
            int maxExtra = 0;
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                if (idx >= mods.size()) break;
                maxExtra = Math.max(maxExtra, (int) cardFor(mods.get(idx)).expandH());
            }
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                if (idx >= mods.size()) break;
                ModernCard card = cardFor(mods.get(idx));
                int cx = contentX + c * (CARD_W + CARD_GAP);
                if (mouseX >= cx && mouseX <= cx + CARD_W
                        && mouseY >= rowY && mouseY <= rowY + CARD_H + card.expandH()) {
                    card.mouseReleased(mouseX, mouseY, cx, rowY, mouseButton);
                }
            }
            yPos += CARD_H + CARD_GAP + maxExtra;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        for (ModernCard c : cards) {
            c.keyTyped(typedChar, keyCode);
        }
        if (!searchFocused) return;
        if (keyCode == Keyboard.KEY_BACK && search.length() > 0) {
            search.deleteCharAt(search.length() - 1);
            scroll = 0;
            animScroll = 0;
            return;
        }
        if (typedChar >= 32 && typedChar < 127) {
            search.append(typedChar);
            scroll = 0;
            animScroll = 0;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
