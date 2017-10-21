package gjum.minecraft.forge.morechunks;

import gjum.minecraft.forge.morechunks.MockChunkServer.ConnCall;
import gjum.minecraft.forge.morechunks.MockMcGame.GameCall;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

public class MoreChunksChunkLoadingTest extends TestCase {

    private MoreChunks moreChunks;
    private MockMcGame game;
    private MockChunkServer chunkServer;
    private IConfig conf;
    private MockEnv env;

    public void setUp() throws Exception {
        super.setUp();
        game = new MockMcGame();
        chunkServer = new MockChunkServer();
        conf = new Config();
        env = new MockEnv();
        moreChunks = new MoreChunks(game, conf, env);
        moreChunks.setChunkServer(chunkServer);
    }

    public void testLoadsExtraChunk() {
        game.ingame = true;
        final Chunk chunk = new Chunk(new Pos2(5, 0), null);
        moreChunks.onReceiveExtraChunk(chunk);
        assertTrue("should load extra chunk",
                game.containsCall(snap ->
                        snap.call == GameCall.LOAD_CHUNK
                                && snap.args[0] == chunk));
    }

    public void testIgnoresExtraChunkWhenNotIngame() {
        game.ingame = false;
        final Chunk chunk = new Chunk(new Pos2(5, 0), null);
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
        final Chunk gameChunk = new Chunk(new Pos2(6, 0), null);
        moreChunks.onReceiveGameChunk(gameChunk);
        game.calls.clear();
        game.loadedChunks.add(gameChunk.pos);

        final Chunk extraChunk = new Chunk(new Pos2(6, 0), null);
        moreChunks.onReceiveExtraChunk(extraChunk);
        assertTrue("Should not load extra chunk if there's already a game chunk there",
                !game.containsCall(GameCall.LOAD_CHUNK));
    }

    public void testForwardsChunkToChunkServer() {
        chunkServer.connected = true;
        final Chunk chunk = new Chunk(new Pos2(2, 3), null);
        moreChunks.onReceiveGameChunk(chunk);
        assertTrue("should forward game chunk to chunk server",
                chunkServer.containsCall(snap ->
                        snap.call == ConnCall.SEND_CHUNK
                                && snap.args[0] == chunk));
    }

    public void testDoesNotForwardChunkIfNotConnectedToChunkServer() {
        chunkServer.connected = false;
        final Chunk chunk = new Chunk(new Pos2(2, 3), null);
        moreChunks.onReceiveGameChunk(chunk);
        assertTrue("should not forward game chunk when not connected to chunk server",
                !chunkServer.containsCall(ConnCall.SEND_CHUNK));
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

    private boolean didRequestForChunkAt(Pos2 pos) {
        return chunkServer.containsCall(snap -> {
            if (snap.call != ConnCall.REQUEST_CHUNKS) return false;
            @SuppressWarnings("unchecked")
            List<Pos2> posList = (List<Pos2>) snap.args[0];
            return posList.contains(pos);
        });
    }

    public void testRequestsExtraChunks() {
        chunkServer.connected = true;
        game.ingame = true;
        moreChunks.onReceiveGameChunk(new Chunk(new Pos2(0, 0), null));

        assertTrue("should request extra chunks outside server's render distance",
                didRequestForChunkAt(new Pos2(5, 0)));

        assertTrue("should not request extra chunks inside server's render distance",
                !didRequestForChunkAt(new Pos2(4, 0)));

        assertTrue("should not request extra chunks outside client's render distance",
                !didRequestForChunkAt(new Pos2(7, 0)));
    }

    public void testDoesNotRequestExtraChunksForLoadedChunks() {
        chunkServer.connected = true;
        game.ingame = true;
        Pos2 alreadyLoaded = new Pos2(5, 1);
        game.loadedChunks.add(alreadyLoaded);

        moreChunks.onReceiveGameChunk(new Chunk(new Pos2(0, 0), null));

        assertTrue("should not request extra chunks for already loaded chunks",
                !didRequestForChunkAt(alreadyLoaded));
    }

    public void testDoesNotRequestExtraChunksWhenNotConnected() {
        chunkServer.connected = false;
        game.ingame = true;
        moreChunks.onReceiveGameChunk(new Chunk(new Pos2(0, 0), null));

        assertTrue("should not request extra chunks when not connected to chunk server",
                !chunkServer.containsCall(ConnCall.REQUEST_CHUNKS));
    }

    public void testUnloadsChunksOverCapOnGameChunkLoad() {
        game.ingame = true;
        conf.setMaxNumChunksLoaded(0);
        Pos2[] chunks = {
                new Pos2(7, 0),
                new Pos2(5, 0),
                new Pos2(3, 0),
        };
        game.loadedChunks.addAll(Arrays.asList(chunks));

        moreChunks.onReceiveGameChunk(new Chunk(new Pos2(0, 0), null));

        for (Pos2 chunk : chunks) {
            assertTrue("Should unload chunks over capacity on game chunk load: " + chunk,
                    game.containsCall(snap ->
                            snap.call == GameCall.UNLOAD_CHUNK
                                    && snap.args[0] == chunk));
        }
    }

    public void testUnloadsChunksOverCapOnExtraChunkLoad() {
        game.ingame = true;
        conf.setMaxNumChunksLoaded(0);
        Pos2[] chunks = {
                new Pos2(7, 0),
                new Pos2(5, 0),
                new Pos2(3, 0),
        };
        game.loadedChunks.addAll(Arrays.asList(chunks));

        moreChunks.onReceiveExtraChunk(new Chunk(new Pos2(5, 0), null));

        for (Pos2 chunk : chunks) {
            assertTrue("Should unload chunks over capacity on game chunk load: " + chunk,
                    game.containsCall(snap ->
                            snap.call == GameCall.UNLOAD_CHUNK
                                    && snap.args[0] == chunk));
        }
    }

    // TODO test when not enabled
}
