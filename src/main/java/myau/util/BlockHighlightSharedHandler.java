package myau.util;

import myau.OpenMyau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.events.TickEvent;
import myau.module.modules.BedESP;
import net.minecraft.client.Minecraft;

/**
 * BLOCK HIGHLIGHT SHARED HANDLER — skidded from Raven B4 (keystrokesmod).
 * Drives SharedBlockHighlightCache from the client event bus:
 * - feeds server chunk/block-change packets into the cache,
 * - consumes the scan queue every client tick (budgeted by BedESP scan speed),
 * - refreshes the cache when a world loads.
 */
public final class BlockHighlightSharedHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    @EventTarget
    public void onReceivePacket(PacketEvent e) {
        if (e.getType() == EventType.RECEIVE) {
            SharedBlockHighlightCache.get().handleReceivePacket(e.getPacket());
        }
    }

    @EventTarget
    public void onClientTick(TickEvent e) {
        if (e.getType() != EventType.POST || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        SharedBlockHighlightCache cache = SharedBlockHighlightCache.get();
        if (!cache.anyConsumerActive()) {
            return;
        }
        int budget = 0;
        if (OpenMyau.moduleManager != null) {
            BedESP bedESP = (BedESP) OpenMyau.moduleManager.modules.get(BedESP.class);
            budget = Math.max(budget, bedESP.getScanSpeedBudget());
        }
        if (budget > 0) {
            cache.tickScan(budget);
        }
    }

    @EventTarget
    public void onWorldLoad(LoadWorldEvent e) {
        SharedBlockHighlightCache cache = SharedBlockHighlightCache.get();
        if (!cache.anyConsumerActive()) {
            return;
        }
        cache.clear();
        cache.enqueueLoadedChunks();
    }
}
