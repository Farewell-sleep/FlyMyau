package myau.module.modules;

import myau.module.Module;
import myau.property.properties.ModeProperty;
import myau.ui.liquid.LiquidClickGui;
import myau.ui.modern.ModernClickGui;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class GuiModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private LiquidClickGui clickGui;
    private ModernClickGui modernGui;

    public final ModeProperty style = new ModeProperty("style", 0, new String[]{"LiquidGlass", "Modern"});

    public GuiModule() {
        super("ClickGui", false);
        setKey(Keyboard.KEY_RSHIFT);
    }

    public void setStyle(int index) {
        this.style.setValue(index);
    }

    @Override
    public void onEnabled() {
        setEnabled(false);
        if (style.getValue() == 1) {
            if (modernGui == null) {
                modernGui = new ModernClickGui();
            }
            mc.displayGuiScreen(modernGui);
        } else {
            if (clickGui == null) {
                clickGui = new LiquidClickGui();
            }
            mc.displayGuiScreen(clickGui);
        }
    }
}
