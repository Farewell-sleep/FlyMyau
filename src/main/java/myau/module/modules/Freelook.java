package myau.module.modules;

import myau.event.EventTarget;
import myau.events.LoadWorldEvent;
import myau.events.PlayerUpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

/**
 * FREELOOK — skidded from Raven B4 (keystrokesmod) into OpenMyau.
 * Third-person free camera: hold the bind (default L) to look around
 * independently of the player. Renders via the freelook mixins in
 * myau.mixin (MixinActiveRenderInfo / MixinRenderGlobal /
 * MixinRenderManager / MixinEntityRenderer / MixinMinecraft).
 */
public class Freelook extends Module {
    public static Freelook instance;

    public static boolean perspectiveToggled;
    public static float cameraYaw;
    public static float cameraPitch;

    private static final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty hold = new BooleanProperty("Hold", true);
    public final BooleanProperty invertPitch = new BooleanProperty("Invert pitch", false);
    public final BooleanProperty lockPitch = new BooleanProperty("Lock pitch", true);
    public final BooleanProperty customFov = new BooleanProperty("Custom FOV", false);
    public final FloatProperty fov = new FloatProperty("FOV", 90.0F, 10.0F, 150.0F, () -> this.customFov.getValue());

    private boolean keyDown;
    private boolean wasKeyDown;

    public Freelook() {
        super("Freelook", false);
        this.setKey(Keyboard.KEY_L);
    }

    @Override
    public void onEnabled() {
        instance = this;
    }

    @Override
    public void onDisabled() {
        instance = null;
        if (perspectiveToggled) {
            resetPerspective();
        }
    }

    @EventTarget
    public void onTick(PlayerUpdateEvent e) {
        if (mc.thePlayer == null) {
            return;
        }
        keyDown = Keyboard.isKeyDown(this.getKey());
        if (keyDown && !wasKeyDown) {
            perspectiveToggled = !perspectiveToggled;
            applyThirdPersonView(perspectiveToggled ? 1 : 0);
        } else if (!keyDown && perspectiveToggled && !hold.getValue()) {
            resetPerspective();
        } else if (!keyDown && perspectiveToggled && hold.getValue() && wasKeyDown) {
            resetPerspective();
        }
        wasKeyDown = keyDown;

        if (perspectiveToggled && mc.currentScreen != null) {
            resetPerspective();
        }
    }

    @EventTarget
    public void onWorldLoad(LoadWorldEvent e) {
        if (perspectiveToggled) {
            resetPerspective();
        }
    }

    public static void resetPerspective() {
        perspectiveToggled = false;
        applyThirdPersonView(0);
    }

    public static void applyThirdPersonView(int mode) {
        if (mode < 0) {
            mode = 0;
        } else if (mode > 2) {
            mode = 2;
        }
        if (mc.getRenderViewEntity() == null && mc.entityRenderer == null) {
            return;
        }
        mc.gameSettings.thirdPersonView = mode;
        if (mc.entityRenderer != null) {
            if (mode == 0) {
                mc.entityRenderer.loadEntityShader(mc.getRenderViewEntity());
            } else if (mode == 1) {
                mc.entityRenderer.loadEntityShader(null);
            }
        }
        if (mc.renderGlobal != null) {
            mc.renderGlobal.setDisplayListEntitiesDirty();
        }
    }

    /**
     * Redirect target for Minecraft.inGameHasFocus reads inside
     * EntityRenderer.updateCameraAndRender. Faithful to Raven B4: when the
     * freelook is not active the original field value is preserved so vanilla
     * (or OptiFine-patched) control flow is never altered.
     */
    public static boolean overrideMouse(Minecraft client) {
        if (!client.inGameHasFocus) {
            return false;
        }
        if (instance == null || !instance.isEnabled() || !perspectiveToggled) {
            return true;
        }

        client.mouseHelper.mouseXYChange();
        float sens = client.gameSettings.mouseSensitivity * 0.6f + 0.2f;
        float mult = sens * sens * sens * 8.0f;
        int dx = ((myau.mixin.IAccessorMouseHelper) client.mouseHelper).getDeltaX();
        int dy = ((myau.mixin.IAccessorMouseHelper) client.mouseHelper).getDeltaY();
        float fdx = dx * mult;
        float fdy = dy * mult;
        cameraYaw += fdx * 0.15f;
        if (instance.invertPitch.getValue()) {
            fdy = -fdy;
        }
        cameraPitch += fdy * 0.15f;
        if (instance.lockPitch.getValue()) {
            cameraPitch = Math.max(-90.0f, Math.min(90.0f, cameraPitch));
        }
        if (instance.customFov.getValue()) {
            client.gameSettings.fovSetting = instance.fov.getValue();
        }
        return false;
    }
}
