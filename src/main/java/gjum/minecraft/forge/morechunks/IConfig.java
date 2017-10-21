package gjum.minecraft.forge.morechunks;

import java.io.File;
import java.io.IOException;

public interface IConfig {
    void blacklistCircle(Pos2 center, int radius);

    void blacklistRectangle(Pos2 corner1, Pos2 corner2);

    boolean canPublishChunk(Pos2 chunkPos);

    boolean getEnabled();

    int getMaxNumChunksLoaded();

    int getPort();

    int getServerRenderDistance();

    String getHostname();

    void load(File configFile) throws IOException;

    void save() throws IOException;

    void save(File configFile) throws IOException;

    void setEnabled(boolean enabled);

    void setHostname(String hostname);

    void setMaxNumChunksLoaded(int maxNumChunksLoaded);

    void setPort(int port);

    void setServerRenderDistance(int serverRenderDistance);
}
