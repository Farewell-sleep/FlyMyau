package myau.mixin;

import myau.module.modules.Freelook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.vector.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * FREELOOK mixin — skidded from Raven B4 MixinRenderGlobal.
 * Applies freelook camera angles to terrain setup (culling + frustum).
 * The PlayerESP outline logic from the original is trimmed (OpenMyau has
 * its own outline implementation).
 */
@SideOnly(Side.CLIENT)
@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {

    @Shadow
    @Final
    private Minecraft mc;

    @Unique
    private boolean freelookIsActive() {
        return Freelook.instance != null && Freelook.instance.isEnabled() && Freelook.perspectiveToggled;
    }

    @Redirect(method = "setupTerrain", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationPitch:F"))
    private float freelookSetupTerrainRotationPitch(Entity entity) {
        if (entity == null) {
            return 0.0F;
        }
        if (entity == mc.getRenderViewEntity() && freelookIsActive()) {
            return Freelook.cameraPitch;
        }
        return entity.rotationPitch;
    }

    @Redirect(method = "setupTerrain", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationYaw:F"))
    private float freelookSetupTerrainRotationYaw(Entity entity) {
        if (entity == null) {
            return 0.0F;
        }
        if (entity == mc.getRenderViewEntity() && freelookIsActive()) {
            return Freelook.cameraYaw;
        }
        return entity.rotationYaw;
    }

    @Redirect(
            method = "setupTerrain",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderGlobal;getViewVector(Lnet/minecraft/entity/Entity;D)Lorg/lwjgl/util/vector/Vector3f;"
            )
    )
    private Vector3f freelookSetupTerrainViewVector(RenderGlobal renderGlobal, Entity entityIn, double partialTicks) {
        if (entityIn == null) {
            return new Vector3f(0.0F, 0.0F, 1.0F);
        }
        float pitch;
        float yaw;
        if (entityIn == mc.getRenderViewEntity() && freelookIsActive()) {
            pitch = Freelook.cameraPitch;
            yaw = Freelook.cameraYaw;
        } else {
            pitch = (float) ((double) entityIn.prevRotationPitch + (double) (entityIn.rotationPitch - entityIn.prevRotationPitch) * partialTicks);
            yaw = (float) ((double) entityIn.prevRotationYaw + (double) (entityIn.rotationYaw - entityIn.prevRotationYaw) * partialTicks);
        }
        if (mc.gameSettings.thirdPersonView == 2) {
            pitch += 180.0F;
        }
        float cosYaw = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float sinYaw = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float cosPitch = -MathHelper.cos(-pitch * 0.017453292F);
        float sinPitch = MathHelper.sin(-pitch * 0.017453292F);
        return new Vector3f(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
    }
}
