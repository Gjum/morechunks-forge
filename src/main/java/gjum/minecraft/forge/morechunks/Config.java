package gjum.minecraft.forge.morechunks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

public class Config implements IConfig {
    @Expose()
    private int chunkLoadsPerSecond;
    @Expose()
    private boolean enabled;
    @Expose()
    private String hostname;
    @Expose()
    private int maxNumChunksLoaded;
    @Expose()
    private int port;
    @Expose()
    private int serverRenderDistance;

    private static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Expose(deserialize = false, serialize = false)
    private File configFile;

    public Config() {
        loadDefaults();
    }

    private void loadDefaults() {
        chunkLoadsPerSecond = 20;
        enabled = true;
        hostname = "gjum.isteinvids.co.uk";
        maxNumChunksLoaded = 16 * 16;
        port = 12312;
        serverRenderDistance = 4;
    }

    // TODO cache blacklist using bitwise filter thing
    // because there will be lots of non-matches, which that data structure is optimized for

    @Override
    public void blacklistCircle(Pos2 center, int radius) {
        // TODO store in source-of-truth, mark blacklist as dirty
    }

    @Override
    public void blacklistRectangle(Pos2 corner1, Pos2 corner2) {
        // TODO store in source-of-truth, mark blacklist as dirty
    }

    @Override
    public boolean canPublishChunk(Pos2 chunkPos) {
        // TODO rebuild blacklist if dirty
        // TODO check against blacklist
        return true;
    }

    @Override
    public int getChunkLoadsPerSecond() {
        return chunkLoadsPerSecond;
    }

    // TODO allow disabling the whole mod
    @Override
    public boolean getEnabled() {
        return enabled;
    }

    @Override
    public String getHostname() {
        return hostname;
    }

    @Override
    public int getMaxNumChunksLoaded() {
        return maxNumChunksLoaded;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public int getServerRenderDistance() {
        return serverRenderDistance;
    }

    @Override
    public void load(File configFile) throws IOException {
        this.configFile = configFile;

        FileReader reader = new FileReader(configFile);
        gson.fromJson(reader, this.getClass());
        reader.close();
    }

    @Override
    public void save() throws IOException {
        save(this.configFile);
    }

    @Override
    public void save(File configFile) throws IOException {
        this.configFile = configFile;
        String json = gson.toJson(this);
        FileOutputStream fos = new FileOutputStream(configFile);
        fos.write(json.getBytes());
        fos.close();
    }

    @Override
    public void setChunkLoadsPerSecond(int chunksPerSec) {
        this.chunkLoadsPerSecond = chunksPerSec;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    @Override
    public void setMaxNumChunksLoaded(int maxNumChunksLoaded) {
        this.maxNumChunksLoaded = maxNumChunksLoaded;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void setServerRenderDistance(int serverRenderDistance) {
        this.serverRenderDistance = serverRenderDistance;
    }
}
