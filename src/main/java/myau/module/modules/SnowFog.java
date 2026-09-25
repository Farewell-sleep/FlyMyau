package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S2BPacketChangeGameState;

/**
 * SnowFog — render-category module driven entirely by the vanilla weather
 * system (equivalent to the vanilla /weather command):
 *   - snow: raises the rain strength so the vanilla weather renderer draws
 *     falling snow (a mixin forces the snow branch of renderRainSnow);
 *     snow-density maps 1:1 to the rain strength.
 *   - fog: the vanilla GL fog is re-tuned to a white snow-fog after
 *     EntityRenderer.setupFog; fog-strength maps to the fog distance.
 * Server weather-change packets (rain/thunder) are cancelled so the client
 * keeps the chosen weather.
 */
public class SnowFog extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty snowDensity = new IntProperty("snow-density", 40, 0, 100);
    public final IntProperty fogStrength = new IntProperty("fog-strength", 35, 0, 100);

    public SnowFog() {
        super("SnowFog", false);
    }

    @Override
    public void onEnabled() {
        applyWeather(true);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) return;
        if (mc.theWorld == null) return;
        applyWeather(true);
    }

    @Override
    public void onDisabled() {
        applyWeather(false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.RECEIVE) return;
        if (!(event.getPacket() instanceof S2BPacketChangeGameState)) return;
        int state = ((S2BPacketChangeGameState) event.getPacket()).getGameState();
        // 1 = begin rain, 2 = end rain, 7 = rain level, 8 = thunder level
        if (state == 1 || state == 2 || state == 7 || state == 8) {
            event.setCancelled(true);
        }
    }

    private void applyWeather(boolean snow) {
        if (mc.theWorld == null) return;
        if (snow) {
            mc.theWorld.setRainStrength(this.snowDensity.getValue() / 100.0F);
            mc.theWorld.getWorldInfo().setRainTime(Integer.MAX_VALUE);
            mc.theWorld.getWorldInfo().setRaining(true);
            mc.theWorld.getWorldInfo().setThunderTime(0);
            mc.theWorld.getWorldInfo().setThundering(false);
        } else {
            mc.theWorld.setRainStrength(0.0F);
            mc.theWorld.getWorldInfo().setRainTime(0);
            mc.theWorld.getWorldInfo().setRaining(false);
            mc.theWorld.getWorldInfo().setThunderTime(0);
            mc.theWorld.getWorldInfo().setThundering(false);
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{"Snow " + this.snowDensity.getValue() + "%", "Fog " + this.fogStrength.getValue() + "%"};
    }
}
