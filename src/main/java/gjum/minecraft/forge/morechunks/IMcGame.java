package gjum.minecraft.forge.morechunks;

import java.util.List;

public interface IMcGame {
    List<Pos2> getLoadedChunks();

    Pos2 getPlayerChunkPos();

    int getRenderDistance();

    boolean isIngame();

    void loadChunk(Chunk chunk);

    void unloadChunk(Pos2 chunkPos);
}
