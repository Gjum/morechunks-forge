package gjum.minecraft.forge.morechunks.gui;

import gjum.minecraft.forge.morechunks.IEnv;
import gjum.minecraft.forge.morechunks.Config;
import gjum.minecraft.forge.morechunks.MoreChunksMod;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.apache.logging.log4j.Level;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

public class GuiConfig extends GuiScreen {
    private final GuiScreen parentScreen;
    private final Config config;
    private final IEnv env;

    private TextField txtAddress;
    private TextField txtChunkLoadsPerSecond;
    private TextField txtMaxNumChunksLoaded;
    private TextField txtServerRenderDistance;
    private GuiButton btnClose;

    private final ArrayList<GuiTextField> textFieldList = new ArrayList<>();
    private int idCounter = 0;

    private static final int DEFAULT_HEIGHT = 18;
    private static final int ROW_HEIGHT = DEFAULT_HEIGHT * 2;
    private static final int FULL_WIDTH = 240;
    private static final int HALF_WIDTH = FULL_WIDTH / 2;
    private static final int THIRD_WIDTH = FULL_WIDTH / 3;

    public GuiConfig(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
        config = MoreChunksMod.instance.config;
        env = MoreChunksMod.instance.env;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        labelList.clear();
        textFieldList.clear();

        final int rowsNum = 4;
        final int topEdge = (height - ROW_HEIGHT * rowsNum) / 2;
        final int leftEdge = (width - FULL_WIDTH) / 2;
        int currentRow = 0;

        GuiLabel title = new GuiLabel(fontRendererObj, nextId(),
                leftEdge, topEdge + currentRow++,
                FULL_WIDTH, DEFAULT_HEIGHT,
                Color.WHITE.getRGB());
        labelList.add(title);
        title.setCentered();
        title.addLine("MoreChunks Settings");

        textFieldList.add(txtChunkLoadsPerSecond = new TextField(nextId(), fontRendererObj,
                leftEdge + 2 * THIRD_WIDTH, topEdge + ROW_HEIGHT * currentRow,
                THIRD_WIDTH,
                "Loading speed", config.getChunkLoadsPerSecond()));
        currentRow++;

        textFieldList.add(txtMaxNumChunksLoaded = new TextField(nextId(), fontRendererObj,
                leftEdge + HALF_WIDTH, topEdge + ROW_HEIGHT * currentRow,
                HALF_WIDTH,
                "Max. Chunks loaded", config.getMaxNumChunksLoaded()));
        currentRow++;

        buttonList.add(btnClose = new GuiButton(nextId(),
                leftEdge + HALF_WIDTH / 2, topEdge + ROW_HEIGHT * currentRow,
                HALF_WIDTH, 20,
                "Save and close"));
        currentRow++;

        if (currentRow != rowsNum) {
            env.log(Level.WARN, "Mismatch in GUI height: expected %s, is %s", rowsNum, currentRow);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        for (GuiTextField txt : textFieldList) {
            txt.drawTextBox();
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void updateScreen() {
        boolean valid = true;

        if (txtAddress.getText().isEmpty()) valid = false;
        try {
            Integer.parseUnsignedInt(txtServerRenderDistance.getText());
            if (!txtChunkLoadsPerSecond.getText().isEmpty()) {
                Integer.parseUnsignedInt(txtChunkLoadsPerSecond.getText());
            }
            if (!txtMaxNumChunksLoaded.getText().isEmpty()) {
                Integer.parseUnsignedInt(txtMaxNumChunksLoaded.getText());
            }
        } catch (NumberFormatException e) {
            valid = false;
        }

        btnClose.enabled = valid;
    }

    @Override
    public void actionPerformed(GuiButton button) {
        if (!button.enabled) return;

        if (button.id == btnClose.id) {
            saveAndLeave();
        }
    }

    @Override
    public void keyTyped(char keyChar, int keyCode) {
        for (GuiTextField txt : textFieldList) {
            if (txt.isFocused()) {
                txt.textboxKeyTyped(keyChar, keyCode);
            }
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            saveAndLeave();
        }
    }

    @Override
    public void mouseClicked(int x, int y, int mouseButton) throws IOException {
        super.mouseClicked(x, y, mouseButton);
        for (GuiTextField field : textFieldList) {
            field.mouseClicked(x, y, mouseButton);
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private void saveAndLeave() {

        if (txtChunkLoadsPerSecond.getText().isEmpty()) {
            config.setChunkLoadsPerSecond(999); // "unlimited"
        } else {
            config.setChunkLoadsPerSecond(Integer.parseUnsignedInt(
                    txtChunkLoadsPerSecond.getText()));
        }

        if (txtMaxNumChunksLoaded.getText().isEmpty()) {
            config.setMaxNumChunksLoaded(999); // "unlimited"
        } else {
            config.setMaxNumChunksLoaded(Integer.parseUnsignedInt(
                    txtMaxNumChunksLoaded.getText()));
        }

        try {
            config.save();
        } catch (IOException e) {
            e.printStackTrace();
            env.log(Level.WARN, "Could not save settings: %s", e.getMessage());
        }

        config.propagateChange();

        mc.displayGuiScreen(parentScreen);
    }

    private int nextId() {
        return idCounter++;
    }
}
