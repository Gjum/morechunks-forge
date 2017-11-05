package gjum.minecraft.forge.morechunks.gui;

import gjum.minecraft.forge.morechunks.IConfig;
import gjum.minecraft.forge.morechunks.IEnv;
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
    private final IConfig config;
    private final IEnv env;

    private GuiLabel labelTitle;
    private TextField txtHostname;
    private TextField txtPort;
    private TextField txtServerRenderDistance;
    private TextField txtChunkLoadsPerSecond;
    private TextField txtMaxNumChunksLoaded;
    private GuiButton btnClose;

    private final ArrayList<GuiTextField> textFieldList = new ArrayList<>();
    private int idCounter = 0;

    private final int DEFAULT_HEIGHT = 20;
    private final int ROW_HEIGHT = DEFAULT_HEIGHT + 15;
    private final int PART_WIDTH = 100;
    private final int FULL_WIDTH = 3 * PART_WIDTH;
    private final int HALF_WIDTH = FULL_WIDTH / 2;

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

        labelList.add(labelTitle = new GuiLabel(fontRendererObj, nextId(),
                leftEdge, topEdge + currentRow++,
                FULL_WIDTH, DEFAULT_HEIGHT,
                Color.WHITE.getRGB()));
        labelTitle.setCentered();

        textFieldList.add(txtHostname = new TextField(nextId(), fontRendererObj,
                leftEdge, topEdge + ROW_HEIGHT * currentRow,
                FULL_WIDTH - PART_WIDTH, DEFAULT_HEIGHT,
                "Hostname", config.getHostname()));
        textFieldList.add(txtPort = new TextField(nextId(), fontRendererObj,
                leftEdge + FULL_WIDTH - PART_WIDTH, topEdge + ROW_HEIGHT * currentRow,
                PART_WIDTH, DEFAULT_HEIGHT,
                "Port", config.getPort()));
        currentRow++;

        textFieldList.add(txtServerRenderDistance = new TextField(nextId(), fontRendererObj,
                leftEdge, topEdge + ROW_HEIGHT * currentRow,
                PART_WIDTH, DEFAULT_HEIGHT,
                "Server view distance", config.getServerRenderDistance()));
        textFieldList.add(txtChunkLoadsPerSecond = new TextField(nextId(), fontRendererObj,
                leftEdge + PART_WIDTH, topEdge + ROW_HEIGHT * currentRow,
                PART_WIDTH, DEFAULT_HEIGHT,
                "Chunk loading speed", config.getChunkLoadsPerSecond()));
        textFieldList.add(txtMaxNumChunksLoaded = new TextField(nextId(), fontRendererObj,
                leftEdge + 2 * PART_WIDTH, topEdge + ROW_HEIGHT * currentRow,
                PART_WIDTH, DEFAULT_HEIGHT,
                "Max. Chunks loaded", config.getMaxNumChunksLoaded()));
        currentRow++;

        buttonList.add(btnClose = new GuiButton(nextId(),
                leftEdge + PART_WIDTH, topEdge + ROW_HEIGHT * currentRow++,
                PART_WIDTH, DEFAULT_HEIGHT,
                "Save and close"));

        if (currentRow != rowsNum) {
            env.log(Level.WARN, "Mismatch in GUI height: expected %s, is %s", rowsNum, currentRow);
        }

        labelTitle.addLine("MoreChunks Settings");
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

        if (txtHostname.getText().isEmpty()) valid = false;
        try {
            Integer.parseUnsignedInt(txtPort.getText());
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
            saveAndLeaveOrStay();
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
            saveAndLeaveOrStay();
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

    private void saveAndLeaveOrStay() {
        config.setHostname(txtHostname.getText().trim());
        config.setPort(Integer.parseUnsignedInt(txtPort.getText()));
        config.setServerRenderDistance(Integer.parseUnsignedInt(txtServerRenderDistance.getText()));

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

        mc.displayGuiScreen(parentScreen);
    }

    private int nextId() {
        return idCounter++;
    }
}
