package gjum.minecraft.forge.morechunks;

public interface IConfig {
    void blacklistCircle(Pos2 center, int radius);

    void blacklistRectangle(Pos2 corner1, Pos2 corner2);

    boolean canPublishChunk(Pos2 chunkPos);

    int getMaxNumChunksLoaded();

    int getPort();

    int getServerRenderDistance();

    String getHostname();

    void setHostname(String hostname);

    void setMaxNumChunksLoaded(int maxNumChunksLoaded);

    void setPort(int port);

    void setServerRenderDistance(int serverRenderDistance);
}
