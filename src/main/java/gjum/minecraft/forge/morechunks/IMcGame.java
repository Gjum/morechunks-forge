package gjum.minecraft.forge.morechunks;

import java.util.List;
import java.util.Map;

public interface IMcGame {
    String getCurrentServerIp();

    Map<Pos2, Long> getChunkLoadTimes();

    List<Pos2> getLoadedChunks();

    Pos2 getPlayerChunkPos();

    int getPlayerDimension();

    int getRenderDistance();

    void insertPacketHandler(IMoreChunks moreChunks);

    boolean isIngame();

    void loadChunk(Chunk chunk);

    void clearChunkCache();

    void runOnMcThread(Runnable runnable);

    void showAchievement(String title, String msg);

    void showChat(String msg);

    void showHotbarMsg(String msg);

    void unloadChunk(Pos2 chunkPos);

    boolean wasPacketHandlerAlreadyInserted();
}
