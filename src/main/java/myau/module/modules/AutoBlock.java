package myau.module.modules;

import myau.OpenMyau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.events.Render2DEvent;
import myau.events.RightClickMouseEvent;
import myau.events.TickEvent;
import myau.mixin.IAccessorEntityPlayer;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.CombatTargeting;
import myau.util.ItemUtil;
import myau.util.KeyBindUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import org.lwjgl.input.Mouse;

/**
 * Skid from Raven B4 (keystrokesmod.module.impl.combat.Autoblock).
 * Event mapping:
 *   onMouse(MouseEvent)                    -> onRightClick (RightClickMouseEvent)
 *   onRightClickMouse/onUseItem            -> covered by onRightClick cancel
 *   onRenderTick(RenderTickEvent)          -> onRender (Render2DEvent, per-frame)
 *   onSendPacket(SendPacketEvent)          -> onPacket (PacketEvent SEND)
 *   onPrePlayerInteract(PrePlayerInteract) -> onTick (TickEvent PRE)
 * Lag system: Raven LagRequest/ModuleBackedTimeout -> OpenMyau.lagManager (outbound packet delay).
 */
public class AutoBlock extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"LAG", "NEW"});
    public final FloatProperty range = new FloatProperty("range", 4.0F, 2.0F, 6.0F);
    public final IntProperty maxHurtTimeMs = new IntProperty("max-hurt-time-ms", 200, 50, 500);
    public final IntProperty maxHoldMs = new IntProperty("max-hold-ms", 150, 50, 500);
    public final FloatProperty aps = new FloatProperty("autoblock-aps", 8.0F, 0.0F, 20.0F);

    public final BooleanProperty requireLmb = new BooleanProperty("require-lmb", true);
    public final BooleanProperty requireRmb = new BooleanProperty("require-rmb", false);
    public final BooleanProperty onlyWhenDamaged = new BooleanProperty("only-when-damaged", false);
    public final BooleanProperty ignoreTeammates = new BooleanProperty("ignore-teammates", true);

    public final PercentProperty lagChance = new PercentProperty("lag-chance", 100);
    public final IntProperty lagMaxDuration = new IntProperty("lag-max-duration", 200, 50, 500);
    public final BooleanProperty preventDelayAttacks = new BooleanProperty("prevent-delay-attacks", true);
    public final BooleanProperty blockAgainImmediately = new BooleanProperty("block-again-immediately", true);
    public final BooleanProperty forceBlockAnimation = new BooleanProperty("force-block-animation", true);

    private boolean isBlocking;
    private boolean manualBlock;
    private int blockStartTick = -1;
    private EntityPlayer currentTarget;
    private int lastSelfHurtTime;

    private boolean isLagging;
    private int lagStartTick = -1;

    private int tickCounter;

    /** NEW mode: ticks remaining in the current attack window (block released). */
    private int releaseTicks;

    public AutoBlock() {
        super("AutoBlock", false);
    }

    @Override
    public void onEnabled() {
        tickCounter = 0;
        resetState(false);
    }

    private static int msToTicks(double ms) {
        if (ms <= 0.0) return 0;
        return (int) Math.ceil(ms / 50.0);
    }

    @Override
    public void onDisabled() {
        resetState(true);
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;
        if (!ItemUtil.isHoldingSword()) return;
        BedNuker bedNuker = (BedNuker) OpenMyau.moduleManager.modules.get(BedNuker.class);
        if (bedNuker != null && bedNuker.isBreaking()) return;
        event.setCancelled(true);
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;
        BedNuker bedNuker = (BedNuker) OpenMyau.moduleManager.modules.get(BedNuker.class);
        if (bedNuker != null && bedNuker.isBreaking()) return;
        if (mc.currentScreen != null && (isBlocking || isLagging)) {
            resetState(true);
            return;
        }
        if (!forceBlockAnimation.getValue() || !ItemUtil.isHoldingSword()) return;
        // Force block animation without touching ItemRenderer (avoids conflicts with
        // Patcher / OverflowAnimations): renderItemInFirstPerson renders the block pose
        // whenever getItemInUseCount() > 0, so we spoof the use count while lagging.
        if (isLagging && mc.thePlayer instanceof IAccessorEntityPlayer) {
            IAccessorEntityPlayer accessor = (IAccessorEntityPlayer) mc.thePlayer;
            if (accessor.getItemInUse() == null) {
                accessor.setItemInUseCount(1);
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || this.mode.getValue() != 0) return;
        if (event.getType() != EventType.SEND) return;
        BedNuker bedNuker = (BedNuker) OpenMyau.moduleManager.modules.get(BedNuker.class);
        if (bedNuker != null && bedNuker.isBreaking()) {
            releaseLag();
            return;
        }
        if (!isLagging || !preventDelayAttacks.getValue()) return;
        if (!(event.getPacket() instanceof C02PacketUseEntity)) return;
        if (((C02PacketUseEntity) event.getPacket()).getAction() != C02PacketUseEntity.Action.ATTACK) return;

        releaseLag();
        if (blockAgainImmediately.getValue() && ItemUtil.isHoldingSword()) {
            startBlocking(tickCounter);
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) return;
        if (this.mode.getValue() == 1) {
            this.onNewModeTick();
            return;
        }
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.isDead || mc.currentScreen != null) {
            resetState(true);
            return;
        }

        BedNuker bedNuker = (BedNuker) OpenMyau.moduleManager.modules.get(BedNuker.class);
        if (bedNuker != null && bedNuker.isBreaking()) {
            resetState(true);
            return;
        }

        int selfHurtTime = mc.thePlayer.hurtTime;
        boolean hurtAgain = selfHurtTime > lastSelfHurtTime;
        lastSelfHurtTime = selfHurtTime;

        if (!ItemUtil.isHoldingSword()) {
            resetState(false);
            return;
        }

        tickCounter++;
        int currentTick = tickCounter;

        currentTarget = CombatTargeting.findTarget(range.getValue() * range.getValue(), ignoreTeammates.getValue());
        KillAura killAura = (KillAura) OpenMyau.moduleManager.modules.get(KillAura.class);
        boolean killAuraAttacking = killAura != null && killAura.isEnabled() && !killAura.requirePress.getValue() && currentTarget != null;
        boolean rmbDown = Mouse.isButtonDown(1);
        boolean lmbDown = Mouse.isButtonDown(0) || killAuraAttacking;

        if (!rmbDown) {
            resetState(true);
            return;
        }

        if (!lmbDown) {
            if (isLagging) releaseLag();
            if (!isBlocking) {
                startBlocking(currentTick);
                manualBlock = true;
            }
            return;
        }

        if (manualBlock) {
            stopBlocking(true);
            manualBlock = false;
        }

        boolean hasTarget = currentTarget != null;
        boolean conditionsMet = hasTarget && checkConditions(lmbDown, rmbDown);

        if (isLagging) {
            int lagMaxTicks = msToTicks(lagMaxDuration.getValue());
            boolean lagExpired = lagMaxTicks > 0 && lagStartTick >= 0 && currentTick - lagStartTick >= lagMaxTicks;

            if (lagExpired || !conditionsMet) {
                releaseLag();
                if (lagExpired && blockAgainImmediately.getValue() && conditionsMet) {
                    startBlocking(currentTick);
                }
            }
        }

        if (!conditionsMet) {
            stopBlocking(true);
            return;
        }

        if (!isBlocking && !isLagging) {
            boolean shouldStart;
            if (onlyWhenDamaged.getValue()) {
                shouldStart = shouldPredictiveBlock();
            } else {
                shouldStart = true;
            }
            if (shouldStart) {
                startBlocking(currentTick);
            }
        }

        if (isBlocking) {
            int maxHoldTicks = msToTicks(maxHoldMs.getValue());
            boolean timeExpired = maxHoldTicks > 0 && blockStartTick >= 0 && currentTick - blockStartTick >= maxHoldTicks;
            boolean shouldStop = timeExpired;
            if (onlyWhenDamaged.getValue() && hurtAgain) {
                shouldStop = true;
            }
            if (shouldStop) {
                if (shouldStartLag()) {
                    startLag(currentTick);
                }
                stopBlocking(true);
            }
        }
    }

    /**
     * NEW mode (skid reference: KillAura auto-block, vanilla style).
     * Hold RMB to keep blocking; while attacking (LMB down or KillAura active)
     * the block is released for a short aps-controlled window so hits register,
     * then re-blocked immediately - i.e. attack and block at the same time.
     */
    private void onNewModeTick() {
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.isDead || mc.currentScreen != null) {
            resetState(true);
            return;
        }
        BedNuker bedNuker = (BedNuker) OpenMyau.moduleManager.modules.get(BedNuker.class);
        if (bedNuker != null && bedNuker.isBreaking()) {
            resetState(true);
            return;
        }
        if (!ItemUtil.isHoldingSword()) {
            resetState(false);
            return;
        }
        boolean rmbDown = Mouse.isButtonDown(1);
        if (!rmbDown) {
            resetState(true);
            return;
        }
        tickCounter++;
        int currentTick = tickCounter;

        KillAura killAura = (KillAura) OpenMyau.moduleManager.modules.get(KillAura.class);
        boolean attacking = Mouse.isButtonDown(0)
                || (killAura != null && killAura.isEnabled() && killAura.getTarget() != null);

        float apsVal = this.aps.getValue();
        if (apsVal > 0.0F && attacking) {
            // attack window: drop the block briefly at aps rate, then re-block
            int window = msToTicks(1000.0F / apsVal);
            window = Math.max(1, Math.min(4, window));
            if (this.releaseTicks > 0) {
                this.releaseTicks--;
                if (this.releaseTicks == 0 && !this.isBlocking) {
                    startBlocking(currentTick);
                }
            } else if (this.isBlocking) {
                stopBlocking(false);
                this.releaseTicks = window;
            }
        } else {
            this.releaseTicks = 0;
            if (!this.isBlocking) {
                startBlocking(currentTick);
            }
        }
    }

    private boolean checkConditions(boolean lmbDown, boolean rmbDown) {
        if (requireLmb.getValue() && !lmbDown) return false;
        if (requireRmb.getValue() && !rmbDown) return false;
        return true;
    }

    private boolean shouldPredictiveBlock() {
        int ourHurtTime = mc.thePlayer.hurtTime;
        int triggerTick = (int) Math.round(maxHurtTimeMs.getValue() / 50.0);
        triggerTick = Math.max(1, Math.min(10, triggerTick));
        return ourHurtTime == triggerTick;
    }

    private void startBlocking(int currentTick) {
        if (!ItemUtil.isHoldingSword()) return;
        int keyCode = mc.gameSettings.keyBindUseItem.getKeyCode();
        KeyBindUtil.setKeyBindState(keyCode, true);
        KeyBindUtil.pressKeyOnce(keyCode);
        isBlocking = true;
        blockStartTick = currentTick;
    }

    private void stopBlocking(boolean forceRelease) {
        if (!isBlocking && !forceRelease) return;
        int keyCode = mc.gameSettings.keyBindUseItem.getKeyCode();
        KeyBindUtil.setKeyBindState(keyCode, false);
        isBlocking = false;
        blockStartTick = -1;
    }

    private boolean shouldStartLag() {
        double chance = lagChance.getValue();
        if (chance <= 0) return false;
        if (chance >= 100) return true;
        return Math.random() * 100 < chance;
    }

    private void startLag(int currentTick) {
        if (isLagging) return;
        int lagReferenceTick = blockStartTick >= 0 ? blockStartTick : currentTick;
        int lagMaxTicks = msToTicks(lagMaxDuration.getValue());
        if (lagMaxTicks > 0 && currentTick - lagReferenceTick >= lagMaxTicks) {
            return;
        }
        OpenMyau.lagManager.setDelay(lagMaxTicks);
        isLagging = true;
        lagStartTick = lagReferenceTick;
    }

    private void releaseLag() {
        if (!isLagging) return;
        OpenMyau.lagManager.setDelay(0);
        isLagging = false;
        lagStartTick = -1;
        if (forceBlockAnimation.getValue() && mc.thePlayer instanceof IAccessorEntityPlayer) {
            IAccessorEntityPlayer accessor = (IAccessorEntityPlayer) mc.thePlayer;
            if (accessor.getItemInUse() == null) {
                accessor.setItemInUseCount(0);
            }
        }
    }

    public boolean isActive() {
        return isEnabled() && (isBlocking || isLagging);
    }

    private void resetState(boolean releaseUseKey) {
        releaseLag();
        stopBlocking(releaseUseKey);
        manualBlock = false;
        if (Mouse.isButtonDown(1) && mc.currentScreen == null) {
            KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
        }
        currentTarget = null;
        lastSelfHurtTime = 0;
        releaseTicks = 0;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
