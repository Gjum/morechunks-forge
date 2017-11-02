package gjum.minecraft.forge.morechunks;

import java.util.ArrayList;
import java.util.Collection;

public class MockMcGame extends CallTracker<MockMcGame.GameCall> implements IMcGame {
    boolean ingame;

    ArrayList<Pos2> loadedChunks = new ArrayList<>();

    enum GameCall {LOAD_CHUNK, UNLOAD_CHUNK, IS_INGAME, GET_LOADED_CHUNKS}

    @Override
    public void loadChunk(Chunk chunk) {
        trackCall(GameCall.LOAD_CHUNK, chunk);
        loadedChunks.add(chunk.pos);
    }

    @Override
    public void unloadChunk(Pos2 chunkPos) {
        trackCall(GameCall.UNLOAD_CHUNK);
        loadedChunks.remove(chunkPos);
    }

    @Override
    public Collection<Pos2> getLoadedChunks() {
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
