package gjum.minecraft.forge.morechunks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MockMcGame extends CallTracker<MockMcGame.GameCall> implements IMcGame {
    public static final String MC_ADDRESS = "mc.example.com";

    String currentServerIp = null;
    boolean isPacketHandlerInPipe = true;
    ArrayList<Pos2> loadedChunks = new ArrayList<>();

    enum GameCall {LOAD_CHUNK, UNLOAD_CHUNK, IS_INGAME, RUN_ON_MC_THREAD, INSERT_PACKET_HANDLER, SHOW_CHAT, SHOW_ACHIEVEMENT, SHOW_HOTBAR_MSG, GET_CHUNK_LOAD_TIMES, GET_LOADED_CHUNKS}

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
    public void showAchievement(String title, String msg) {
        trackCall(GameCall.SHOW_ACHIEVEMENT, title, msg);
    }

    @Override
    public void showChat(String msg) {
        trackCall(GameCall.SHOW_CHAT, msg);
    }

    @Override
    public void showHotbarMsg(String msg) {
        trackCall(GameCall.SHOW_HOTBAR_MSG, msg);
    }

    @Override
    public void unloadChunk(Pos2 chunkPos) {
        trackCall(GameCall.UNLOAD_CHUNK, chunkPos);
        loadedChunks.remove(chunkPos);
    }

    @Override
    public boolean wasPacketHandlerAlreadyInserted() {
        return isPacketHandlerInPipe;
    }

    @Override
    public String getCurrentServerIp() {
        return currentServerIp;
    }

    @Override
    public Map<Pos2, Long> getChunkLoadTimes() {
        trackCall(GameCall.GET_CHUNK_LOAD_TIMES);
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
    public int getPlayerDimension() {
        return 0;
    }

    @Override
    public int getRenderDistance() {
        return 6;
    }

    @Override
    public void insertPacketHandler(IMoreChunks moreChunks) {
        trackCall(GameCall.INSERT_PACKET_HANDLER, moreChunks);
        isPacketHandlerInPipe = true;
    }

    @Override
    public boolean isIngame() {
        trackCall(GameCall.IS_INGAME);
        return currentServerIp != null;
    }
}
