package gjum.minecraft.forge.morechunks.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

import java.awt.*;

public class TextField extends GuiTextField {
    private static final int xOffset = 4;

    private final String label;
    private final FontRenderer fontRenderer;

    public TextField(int id, FontRenderer fontRenderer, int x, int y, int width, int height, String label, String initialValue) {
        super(id, fontRenderer, x, y + fontRenderer.FONT_HEIGHT, width, height);

        this.fontRenderer = fontRenderer;
        this.label = label;
        setText(initialValue);
    }

    public TextField(int id, FontRenderer fontRenderer, int x, int y, int width, int height, String label, int initialValue) {
        super(id, fontRenderer, x, y + fontRenderer.FONT_HEIGHT, width, height);

        this.fontRenderer = fontRenderer;
        this.label = label;
        setText(String.valueOf(initialValue));

        setValidator(s -> {
            if (s == null || s.isEmpty()) return true;
            try {
                Integer.parseInt(s);
                return true;
            } catch (NumberFormatException ignored) {
                return false;
            }
        });
    }

    @Override
    public void drawTextBox() {
        super.drawTextBox();

        if (!this.getVisible()) return;

        int labelWidth = fontRenderer.getStringWidth(label);

        drawRect(
                xPosition + xOffset,
                yPosition - fontRenderer.FONT_HEIGHT,
                xPosition + xOffset + labelWidth,
                yPosition,
                Color.BLACK.getRGB());

        fontRenderer.drawString(label,
                xPosition + xOffset, yPosition - fontRenderer.FONT_HEIGHT,
                Color.WHITE.getRGB());
    }
}
