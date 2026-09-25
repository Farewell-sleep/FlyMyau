package myau.mixin;

import net.minecraft.util.MouseHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * ACCESSOR — skidded from Raven B4 IAccessorMouseHelper.
 * Exposes the raw mouse delta fields consumed by Freelook.overrideMouse.
 */
@Mixin(MouseHelper.class)
public interface IAccessorMouseHelper {

    @Accessor("deltaX")
    int getDeltaX();

    @Accessor("deltaY")
    int getDeltaY();
}
