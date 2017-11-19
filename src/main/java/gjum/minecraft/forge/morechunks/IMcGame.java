package gjum.minecraft.forge.morechunks;

import java.util.List;

public interface IMcGame {
    String getCurrentServerIp();

    List<Pos2> getLoadedChunks();

    Pos2 getPlayerChunkPos();

    int getPlayerDimension();

    int getRenderDistance();

    void insertPacketHandler(IMoreChunks moreChunks);

    boolean isIngame();

    void loadChunk(Chunk chunk);

    void runOnMcThread(Runnable runnable);

    void unloadChunk(Pos2 chunkPos);

    boolean wasPacketHandlerAlreadyInserted();
}
