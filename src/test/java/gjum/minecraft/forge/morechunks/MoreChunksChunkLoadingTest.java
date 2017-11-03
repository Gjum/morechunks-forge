package gjum.minecraft.forge.morechunks;

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
        Chunk chunk = new Chunk(new Pos2(0, 0), null);
        moreChunks.onReceiveExtraChunk(chunk);
        assertEquals(GameCall.LOAD_CHUNK, game.getLastCall().call);
        assertEquals(chunk, game.getLastCall().args[0]);
    }

    public void testIgnoresExtraChunkWhenNotIngame() {
        game.ingame = false;
        Chunk chunk = new Chunk(new Pos2(0, 0), null);
        moreChunks.onReceiveExtraChunk(chunk);
        assertFalse(game.containsCall(GameCall.LOAD_CHUNK));
    }

    public void testIgnoresExtraChunkWhenOutsideRenderDistance() {
        game.ingame = true;
        Chunk chunk = new Chunk(new Pos2(6, 6), null);
        moreChunks.onReceiveExtraChunk(chunk);
        assertFalse(game.containsCall(GameCall.LOAD_CHUNK));
    }
}
