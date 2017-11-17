package gjum.minecraft.forge.morechunks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Config {
    @Expose()
    private boolean modEnabled = true;
    @Expose()
    private int chunkLoadsPerSecond = 40;
    @Expose()
    private int maxNumChunksLoaded = 512;
    @Expose
    private Map<String, McServerConfig> mcServerConfigs = new HashMap<>();

    private final Collection<IMoreChunks> subscribers = new ArrayList<>();

    private File configFile;

    public static final String CIV_CLASSIC_ADDRESS = "mc.civclassic.com";

    private static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();

    public Config() {
        putMcServerConfig(new McServerConfig(CIV_CLASSIC_ADDRESS, true, "gjum.isteinvids.co.uk:12312", 4));
    }

    public void addSubscriber(IMoreChunks subscriber) {
        subscribers.add(subscriber);
    }

    // TODO cache blacklist using bitwise filter thing
    // because there will be lots of non-matches, which that data structure is optimized for

    public void blacklistCircle(Pos2 center, int radius) {
        // TODO store in source-of-truth, mark blacklist as dirty
    }

    public void blacklistRectangle(Pos2 corner1, Pos2 corner2) {
        // TODO store in source-of-truth, mark blacklist as dirty
    }

    public boolean canPublishChunk(Pos2 chunkPos) {
        // TODO rebuild blacklist if dirty
        // TODO check against blacklist
        return true;
    }

    public int getChunkLoadsPerSecond() {
        return chunkLoadsPerSecond;
    }

    public int getMaxNumChunksLoaded() {
        return maxNumChunksLoaded;
    }

    public McServerConfig getMcServerConfig(String serverAddress) {
        return mcServerConfigs.get(serverAddress);
    }

    public Set<String> getMcServerKeys() {
        return mcServerConfigs.keySet();
    }

    public boolean isModEnabled() {
        return modEnabled;
    }

    public void load(File configFile) throws IOException {
        FileReader reader = new FileReader(configFile);
        copyFrom(gson.fromJson(reader, this.getClass()));
        reader.close();
        this.configFile = configFile;
    }

    private void copyFrom(Config newConf) {
        setModEnabled(newConf.isModEnabled());
        setChunkLoadsPerSecond(newConf.getChunkLoadsPerSecond());
        setMaxNumChunksLoaded(newConf.getMaxNumChunksLoaded());

        mcServerConfigs.clear();
        for (String address : newConf.getMcServerKeys()) {
            putMcServerConfig(newConf.getMcServerConfig(address));
        }
    }

    public void save() throws IOException {
        save(configFile);
    }

    public void save(File configFile) throws IOException {
        String json = gson.toJson(this);
        FileOutputStream fos = new FileOutputStream(configFile);
        fos.write(json.getBytes());
        fos.close();

        this.configFile = configFile;
    }

    public void propagateChange() {
        for (IMoreChunks subscriber : subscribers) {
            subscriber.onConfigChanged();
        }
    }

    public void putMcServerConfig(McServerConfig mcServerConfig) {
        mcServerConfigs.put(mcServerConfig.mcServerAddress, mcServerConfig);
    }

    public void setChunkLoadsPerSecond(int chunksPerSec) {
        this.chunkLoadsPerSecond = chunksPerSec;
    }

    public void setMaxNumChunksLoaded(int maxNumChunksLoaded) {
        this.maxNumChunksLoaded = maxNumChunksLoaded;
    }

    public void setModEnabled(boolean modEnabled) {
        this.modEnabled = modEnabled;
    }
}
