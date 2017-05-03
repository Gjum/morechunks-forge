package gjum.minecraft.forge.chunkfix.config;


import gjum.minecraft.forge.chunkfix.ChunkFix;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;
import java.util.Set;

public class ChunkFixConfig {
    public static final String CATEGORY_MAIN = "Main";

    public static final gjum.minecraft.forge.chunkfix.config.ChunkFixConfig instance = new gjum.minecraft.forge.chunkfix.config.ChunkFixConfig();

    public Configuration config;

    private boolean enabled, filterGlitchChunks, keepChunksLoaded, loadExtraChunks, publishChunks;
    private Property propEnabled, propFilterGlitchChunks, propKeepChunksLoaded, propLoadExtraChunks, propPublishChunks;

    private String hostname;
    private int port;
    private Property propHostname, propPort;

    private ChunkFixConfig() {
    }

    public void load(File configFile) {
        config = new Configuration(configFile, gjum.minecraft.forge.chunkfix.ChunkFixMod.VERSION);

        syncProperties();
        final ConfigCategory categoryMain = config.getCategory(CATEGORY_MAIN);
        final Set<String> confKeys = categoryMain.keySet();

        config.load();

        if (!config.getDefinedConfigVersion().equals(config.getLoadedConfigVersion())) {
            // clear config from old entries
            // otherwise they would clutter the gui
            final Set<String> unusedConfKeys = categoryMain.keySet();
            unusedConfKeys.removeAll(confKeys);
            for (String confKey : unusedConfKeys) {
                categoryMain.remove(confKey);
            }
        }

        syncProperties();
        syncValues();
    }

    public void afterGuiSave() {
        syncProperties();
        syncValues();
    }

    public void setEnabled(boolean enabled) {
        syncProperties();
        propEnabled.set(enabled);
        syncValues();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isFilterGlitchChunks() {
        return enabled && filterGlitchChunks;
    }

    public boolean isKeepChunksLoaded() {
        return enabled && keepChunksLoaded;
    }

    public boolean isLoadExtraChunks() {
        return enabled && loadExtraChunks;
    }

    /**
     * Should only be used when also checking blacklisted regions.
     * Instead, use canPublishChunk.
     */
    private boolean isPublishChunks() {
        return enabled && publishChunks;
    }

    public boolean canPublishChunk(int chunkX, int chunkZ) {
        if (!isPublishChunks()) return false;
        return true; // TODO blacklist regions
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    /**
     * no idea why this has to be called so often, ideally the prop* would stay the same,
     * but it looks like they get disassociated from the config sometimes and setting them no longer has any effect
     */
    private void syncProperties() {
        propEnabled = config.get(CATEGORY_MAIN, "enabled", true, "");
        propFilterGlitchChunks = config.get(CATEGORY_MAIN, "filterGlitchChunks", true, "Discard glitched chunk packets.");
        propKeepChunksLoaded = config.get(CATEGORY_MAIN, "keepChunksLoaded", true, "Do not unload chunks when the server unloads them.");
        propLoadExtraChunks = config.get(CATEGORY_MAIN, "loadExtraChunks", true, "Load chunks outside the server render distance.");
        propPublishChunks = config.get(CATEGORY_MAIN, "publishChunks", true, "Store extra chunks to be loaded in the future.");

        propHostname = config.get(CATEGORY_MAIN, "chunkserverHostname", "gjum.isteinvids.co.uk", "Address of the chunk server.");
        propPort = config.get(CATEGORY_MAIN, "chunkserverPort", 12312, "Port of the chunk server.");
    }

    /**
     * called every time a prop is changed, to apply the new values to the fields and to save the values to the config file
     */
    private void syncValues() {
        enabled = propEnabled.getBoolean();
        filterGlitchChunks = propFilterGlitchChunks.getBoolean();
        keepChunksLoaded = propKeepChunksLoaded.getBoolean();
        loadExtraChunks = propLoadExtraChunks.getBoolean();
        publishChunks = propPublishChunks.getBoolean();

        hostname = propHostname.getString();
        port = propPort.getInt();

        // TODO if turning off, unload extra chunks

        if (config.hasChanged()) {
            config.save();
            syncProperties();
            ChunkFix.log("Saved config.");
        }
    }
}
