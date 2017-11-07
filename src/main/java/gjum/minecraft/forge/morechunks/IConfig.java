package gjum.minecraft.forge.morechunks;

import java.io.File;
import java.io.IOException;

public interface IConfig {
    void blacklistCircle(Pos2 center, int radius);

    void blacklistRectangle(Pos2 corner1, Pos2 corner2);

    boolean canPublishChunk(Pos2 chunkPos);

    int getChunkLoadsPerSecond();

    String getChunkServerAddress();

    boolean getEnabled();

    int getMaxNumChunksLoaded();

    int getServerRenderDistance();

    void load(File configFile) throws IOException;

    void save() throws IOException;

    void save(File configFile) throws IOException;

    void setChunkLoadsPerSecond(int chunksPerSec);

    void setChunkServerAddress(String address);

    void setEnabled(boolean enabled);

    void setMaxNumChunksLoaded(int maxNumChunksLoaded);

    void setServerRenderDistance(int serverRenderDistance);
}
