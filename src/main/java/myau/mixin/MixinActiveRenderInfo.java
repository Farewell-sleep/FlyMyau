package myau.mixin;

import myau.module.modules.Freelook;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * FREELOOK mixin — skidded from Raven B4 MixinActiveRenderInfo.
 * Feeds the freelook camera angles into the render info used for culling.
 */
@SideOnly(Side.CLIENT)
@Mixin(ActiveRenderInfo.class)
public class MixinActiveRenderInfo {

    @Redirect(method = "updateRenderInfo", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationYaw:F"))
    private static float freelookRedirectRotationYaw(Entity entity) {
        if (entity == null) {
            return 0.0F;
        }
        if (Freelook.instance != null && Freelook.instance.isEnabled() && Freelook.perspectiveToggled && entity instanceof EntityPlayer) {
            return Freelook.cameraYaw;
        }
        return entity.rotationYaw;
    }

    @Redirect(method = "updateRenderInfo", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationPitch:F"))
    private static float freelookRedirectRotationPitch(Entity entity) {
        if (entity == null) {
            return 0.0F;
        }
        if (Freelook.instance != null && Freelook.instance.isEnabled() && Freelook.perspectiveToggled && entity instanceof EntityPlayer) {
            return Freelook.cameraPitch;
        }
        return entity.rotationPitch;
    }
}
