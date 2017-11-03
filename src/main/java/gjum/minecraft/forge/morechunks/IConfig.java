package gjum.minecraft.forge.morechunks;

public interface IConfig {
    void blacklistCircle(Pos2 center, int radius);

    void blacklistRectangle(Pos2 corner1, Pos2 corner2);

    boolean canPublishChunk(Pos2 chunkPos);

    int getMaxNumChunksLoaded();

    int getServerRenderDistance();

    void setMaxNumChunksLoaded(int maxNumChunksLoaded);

    void setServerRenderDistance(int serverRenderDistance);
}
