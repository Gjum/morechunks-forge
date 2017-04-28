package gjum.minecraft.forge.chunkfix.config;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.List;

public class ConfigGui extends GuiConfig {

    public ConfigGui(GuiScreen parent) {
        super(parent, getConfigElements(),
                gjum.minecraft.forge.chunkfix.ChunkFixMod.MOD_ID, false, false, "ChunkFix config");
    }

    private static List<IConfigElement> getConfigElements() {
        Configuration config = ChunkFixConfig.instance.config;
        return new ConfigElement(config.getCategory(ChunkFixConfig.CATEGORY_MAIN)).getChildElements();
    }

}
