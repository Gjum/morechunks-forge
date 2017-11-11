package gjum.minecraft.forge.morechunks;

import java.util.ArrayList;
import java.util.List;

public class MockMcGame extends CallTracker<MockMcGame.GameCall> implements IMcGame {
    boolean ingame;

    ArrayList<Pos2> loadedChunks = new ArrayList<>();

    enum GameCall {LOAD_CHUNK, UNLOAD_CHUNK, IS_INGAME, RUN_ON_MC_THREAD, GET_LOADED_CHUNKS}

    @Override
    public void loadChunk(Chunk chunk) {
        trackCall(GameCall.LOAD_CHUNK, chunk);
        loadedChunks.add(chunk.pos);
    }

    @Override
    public void runOnMcThread(Runnable runnable) {
        trackCall(GameCall.RUN_ON_MC_THREAD, runnable);
        runnable.run();
    }

    @Override
    public void unloadChunk(Pos2 chunkPos) {
        trackCall(GameCall.UNLOAD_CHUNK, chunkPos);
        loadedChunks.remove(chunkPos);
    }

    @Override
    public List<Pos2> getLoadedChunks() {
        trackCall(GameCall.GET_LOADED_CHUNKS);
        return loadedChunks;
    }

    @Override
    public Pos2 getPlayerChunkPos() {
        return new Pos2(0, 0);
    }

    @Override
    public int getRenderDistance() {
        return 5;
    }

    @Override
    public boolean isIngame() {
        trackCall(GameCall.IS_INGAME);
        return ingame;
    }
}
