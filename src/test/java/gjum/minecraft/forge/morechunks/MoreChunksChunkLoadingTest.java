package gjum.minecraft.forge.morechunks;

import gjum.minecraft.forge.morechunks.MockChunkServerConnection.ConnCall;
import gjum.minecraft.forge.morechunks.MockMcGame.GameCall;
import junit.framework.TestCase;

public class MoreChunksChunkLoadingTest extends TestCase {

    private MoreChunks moreChunks;
    private MockMcGame game;
    private MockChunkServerConnection conn;
    private IConfig conf;
    private MockEnv env;

    public void setUp() throws Exception {
        super.setUp();
        game = new MockMcGame();
        conn = new MockChunkServerConnection();
        conf = new MoreChunksConfig();
        env = new MockEnv();
        moreChunks = new MoreChunks(game, conn, conf, env);
    }

    public void testLoadsExtraChunk() {
        game.ingame = true;
        final Chunk chunk = new Chunk(new Pos2(0, 0), null);
        moreChunks.onReceiveExtraChunk(chunk);
        assertTrue("should load extra chunk",
                game.containsCall(snap ->
                        snap.call == GameCall.LOAD_CHUNK
                                && snap.args[0] == chunk));
    }

    public void testIgnoresExtraChunkWhenNotIngame() {
        game.ingame = false;
        final Chunk chunk = new Chunk(new Pos2(0, 0), null);
        moreChunks.onReceiveExtraChunk(chunk);
        assertFalse(game.containsCall(GameCall.LOAD_CHUNK));
    }

    public void testIgnoresExtraChunkWhenOutsideRenderDistance() {
        game.ingame = true;
        final Chunk chunk = new Chunk(new Pos2(6, 6), null);
        moreChunks.onReceiveExtraChunk(chunk);
        assertFalse(game.containsCall(GameCall.LOAD_CHUNK));
    }

    public void testIgnoresExtraChunkWhenHasGameChunkThere() {
        game.ingame = true;
        final Chunk gameChunk = new Chunk(new Pos2(0, 0), null);
        moreChunks.onReceiveGameChunk(gameChunk);
        game.calls.clear();
        game.loadedChunks.add(gameChunk.pos);

        final Chunk extraChunk = new Chunk(new Pos2(0, 0), null);
        moreChunks.onReceiveExtraChunk(extraChunk);
        assertTrue("Should not load extra chunk if there's already a game chunk there",
                !game.containsCall(GameCall.LOAD_CHUNK));
    }

    public void testForwardsChunkToChunkServer() {
        conn.connected = true;
        final Chunk chunk = new Chunk(new Pos2(2, 3), null);
        moreChunks.onReceiveGameChunk(chunk);
        assertTrue("should forward game chunk to chunk server",
                conn.containsCall(snap ->
                        snap.call == ConnCall.SEND_CHUNK
                                && snap.args[0] == chunk));
    }

    public void testDoesNotForwardChunkIfNotConnectedToChunkServer() {
        conn.connected = false;
        final Chunk chunk = new Chunk(new Pos2(2, 3), null);
        moreChunks.onReceiveGameChunk(chunk);
        assertTrue("should not forward game chunk when not connected to chunk server",
                !conn.containsCall(ConnCall.SEND_CHUNK));
    }

    public void testOnGameChunkUnloadFarButKeepCloseChunks() {
        Pos2 nearPos = new Pos2(5, 5);
        Pos2 farPos = new Pos2(6, 0);
        game.loadedChunks.add(nearPos);
        game.loadedChunks.add(farPos);

        moreChunks.onReceiveGameChunk(new Chunk(new Pos2(2, 3), null));

        assertTrue("should unload chunks outside render distance",
                game.containsCall(snap ->
                        snap.call == GameCall.UNLOAD_CHUNK
                                && snap.args[0] == farPos));

        assertTrue("should not unload chunks within render distance",
                !game.containsCall(snap ->
                        snap.call == GameCall.UNLOAD_CHUNK
                                && snap.args[0] == nearPos));
    }
}
