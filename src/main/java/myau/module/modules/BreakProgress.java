package myau.module.modules;

import myau.OpenMyau;
import myau.event.EventTarget;
import myau.events.PlayerUpdateEvent;
import myau.events.Render3DEvent;
import myau.mixin.IAccessorPlayerControllerMP;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import org.lwjgl.opengl.GL11;

/**
 * BREAK PROGRESS — skidded from Raven B4 (keystrokesmod), verbatim logic:
 * shows a progress label on the block currently being broken (manual or
 * BedNuker target). Modes: Percentage / Time remaining / Decimal.
 */
public class BreakProgress extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Percentage", "Time", "Decimal"});
    public final BooleanProperty manual = new BooleanProperty("Show manual", true);
    public final BooleanProperty bedAura = new BooleanProperty("Show bedAura", true);
    public final BooleanProperty fadeIn = new BooleanProperty("Fade in", false);

    private BlockPos block;
    private float progress;
    private String progressStr = "";

    public BreakProgress() {
        super("BreakProgress", false);
    }

    @EventTarget
    public void onUpdate(PlayerUpdateEvent event) {
        if (!isEnabled()) {
            return;
        }
        if (mc.thePlayer.capabilities.isCreativeMode || !mc.thePlayer.capabilities.allowEdit) {
            this.resetVariables();
            return;
        }

        if (bedAura.getValue() && OpenMyau.moduleManager != null) {
            BedNuker bedNuker = (BedNuker) OpenMyau.moduleManager.modules.get(BedNuker.class);
            if (bedNuker != null && bedNuker.isEnabled()) {
                BlockPos auraTarget = bedNuker.getAuraTargetPos();
                float auraProgress = bedNuker.getAuraBreakProgress();
                if (auraTarget != null && auraProgress > 0.0F) {
                    this.progress = Math.min(1.0F, auraProgress);
                    this.block = auraTarget;
                    this.setProgress();
                    return;
                }
            }
        }

        if (!manual.getValue() || mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectType.BLOCK) {
            this.resetVariables();
            return;
        }

        this.progress = ((IAccessorPlayerControllerMP) mc.playerController).getCurBlockDamageMP();
        if (this.progress == 0.0F) {
            this.resetVariables();
            return;
        }

        this.block = mc.objectMouseOver.getBlockPos();
        this.setProgress();
    }

    private void setProgress() {
        switch (this.mode.getValue()) {
            case 0:
                this.progressStr = (int) (100.0 * (this.progress / 1.0)) + "%";
                break;
            case 1: {
                double timeLeft = round((1.0F - this.progress) / getBlockHardness(getBlock(this.block), mc.thePlayer.getHeldItem()) / 20.0, 1);
                this.progressStr = timeLeft == 0 ? "0" : timeLeft + "s";
                break;
            }
            case 2:
                this.progressStr = String.valueOf(round(this.progress, 2));
                break;
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.progress == 0.0F || this.block == null || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        double x = this.block.getX() + 0.5 - mc.getRenderManager().viewerPosX;
        double y = this.block.getY() + 0.5 - mc.getRenderManager().viewerPosY;
        double z = this.block.getZ() + 0.5 - mc.getRenderManager().viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((mc.gameSettings.thirdPersonView == 2 ? -1 : 1) * mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-0.02266667F, -0.02266667F, -0.02266667F);
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GL11.glEnable(GL11.GL_BLEND);

        int colorAlpha = (-1 & 0xFFFFFF) | (Math.max(10, (int) (255 * progress)) << 24);
        mc.fontRendererObj.drawString(
                this.progressStr,
                (float) (-mc.fontRendererObj.getStringWidth(this.progressStr) / 2),
                -3.0F,
                fadeIn.getValue() ? colorAlpha : -1,
                true
        );

        GL11.glDisable(GL11.GL_BLEND);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private void resetVariables() {
        this.block = null;
        this.progress = 0.0F;
        this.progressStr = "";
    }

    private static double round(double value, int places) {
        double pow = Math.pow(10.0, places);
        return Math.round(value * pow) / pow;
    }

    /** Raven BlockUtils.getBlock(BlockPos) — inlined. */
    private static Block getBlock(BlockPos pos) {
        return mc.theWorld.getBlockState(pos).getBlock();
    }

    /** Raven BlockUtils.getBlockHardness(b, stack, false, false) — inlined. */
    private static float getBlockHardness(Block b, ItemStack stack) {
        if (b == null) {
            return 1.0F;
        }
        float hardness = b.getBlockHardness(mc.theWorld, mc.thePlayer.getPosition());
        if (hardness < 0.0F) {
            return 0.0F;
        }
        float digSpeed = 1.0F;
        if (stack != null) {
            digSpeed = stack.getStrVsBlock(b);
            if (digSpeed > 1.0F) {
                int efficiency = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack);
                if (efficiency > 0) {
                    digSpeed += efficiency * efficiency + 1;
                }
            }
        }
        return digSpeed / hardness / 100.0F;
    }
}
