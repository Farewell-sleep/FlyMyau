package myau.module.modules;

import myau.module.Module;
import myau.property.properties.FloatProperty;

/**
 * ITEM PHYSICS — skidded from Raven B4 (keystrokesmod) into OpenMyau.
 * Adds custom in-air item rotation physics. The actual rendering is done
 * in myau.mixin.MixinRenderEntityItem which reads ItemPhysics.instance.
 */
public class ItemPhysics extends Module {
    public static ItemPhysics instance;

    public final FloatProperty rotationSpeed;

    public ItemPhysics() {
        super("Item Physics", false);
        this.rotationSpeed = new FloatProperty("Rotation speed", 1.0F, 0.0F, 5.0F);
    }

    @Override
    public void onEnabled() {
        instance = this;
    }

    @Override
    public void onDisabled() {
        instance = null;
    }
}
