package gjum.minecraft.forge.morechunks.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

import java.awt.*;

public class TextField extends GuiTextField {
    private static final int MARGIN = 2;

    private final String label;
    private final FontRenderer fontRenderer;

    public TextField(int id, FontRenderer fontRenderer, int x, int y, int width, String label, String initialValue) {
        super(id, fontRenderer, x + MARGIN, y + MARGIN + fontRenderer.FONT_HEIGHT, width - 2 * MARGIN, 18 - 2 * MARGIN);

        this.fontRenderer = fontRenderer;
        this.label = label;
        setText(initialValue);
    }

    public TextField(int id, FontRenderer fontRenderer, int x, int y, int width, String label, int initialValue) {
        super(id, fontRenderer, x + MARGIN, y + MARGIN + fontRenderer.FONT_HEIGHT, width - 2 * MARGIN, 18 - 2 * MARGIN);

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
                xPosition - 1,
                yPosition - fontRenderer.FONT_HEIGHT - 1,
                xPosition + labelWidth,
                yPosition,
                Color.LIGHT_GRAY.getRGB());

        fontRenderer.drawString(label,
                xPosition, yPosition - fontRenderer.FONT_HEIGHT,
                Color.BLACK.getRGB());
    }
}
