package gjum.minecraft.forge.morechunks;

import gjum.minecraft.forge.morechunks.MockChunkServer.ConnCall;
import junit.framework.TestCase;

public class MoreChunksConnectionTest extends TestCase {

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

    public void testConnectsChunkServerOnJoinGame() {
        chunkServer.connected = false;
        game.ingame = true;
        moreChunks.onGameConnected();
        assertTrue(chunkServer.containsCall(ConnCall.CONNECT));
    }

    public void testDisconnectsChunkServerOnLeaveGame() {
        chunkServer.connected = true;
        moreChunks.onGameDisconnected();
        assertEquals(ConnCall.DISCONNECT, chunkServer.getLastCall().call);
        assertEquals("MoreChunks: Game ending", ((DisconnectReason) chunkServer.getLastCall().args[0]).description);
    }

    public void testReconnectsOnChunkServerDisconnectWhenIngame() {
        chunkServer.connected = false;
        game.ingame = true;
        moreChunks.onChunkServerDisconnected(new DisconnectReason("Test"));
        assertEquals(ConnCall.CONNECT, chunkServer.getLastCall().call);
    }

    public void testNoReconnectionOnChunkServerDisconnectWhenNotIngame() {
        chunkServer.connected = false;
        game.ingame = false;
        moreChunks.onChunkServerDisconnected(new DisconnectReason("Test"));
        assertFalse(chunkServer.containsCall(ConnCall.CONNECT));
    }

    public void testDisconnectsChunkServerOnConnectWhenNotIngame() {
        chunkServer.connected = true;
        game.ingame = false;
        moreChunks.onChunkServerConnected();
        assertEquals(ConnCall.DISCONNECT, chunkServer.getLastCall().call);
        assertEquals("MoreChunks: No game running", ((DisconnectReason) chunkServer.getLastCall().args[0]).description);
    }

    public void testNoReconnectOnConnectChunkServerWhenNotIngame() {
        chunkServer.connected = true;
        game.ingame = true;
        moreChunks.onChunkServerConnected();
        assertTrue(chunkServer.calls.isEmpty());
    }

    public void testReconnectWithExponentialBackoff() {
        game.ingame = true;
        chunkServer.connected = false;

        env.nowMs = 0;
        moreChunks.onChunkServerDisconnected(new DisconnectReason("Test"));
        assertTrue("first reconnect attempt should happen instantly",
                chunkServer.containsCall(ConnCall.CONNECT));

        chunkServer.calls.clear();
        env.nowMs = 1;
        moreChunks.onTick();
        assertTrue("should not reconnect while waiting for timeout",
                !chunkServer.containsCall(ConnCall.CONNECT));

        moreChunks.onChunkServerDisconnected(new DisconnectReason("Test"));
        assertTrue("second reconnect attempt should not happen instantly",
                !chunkServer.containsCall(ConnCall.CONNECT));

        final long firstTimeout = 1000;

        chunkServer.calls.clear();
        env.nowMs = firstTimeout - 1;
        moreChunks.onTick();
        assertTrue("second reconnect attempt should not happen before timeout",
                !chunkServer.containsCall(ConnCall.CONNECT));

        chunkServer.calls.clear();
        env.nowMs = firstTimeout;
        moreChunks.onTick();
        assertTrue("second reconnect attempt should happen after timeout",
                chunkServer.containsCall(ConnCall.CONNECT));

        final long secondTimeout = firstTimeout + 2 * firstTimeout;
        chunkServer.calls.clear();
        env.nowMs = secondTimeout - 1;
        moreChunks.onTick();
        assertTrue("third reconnect attempt should not happen before double timeout",
                !chunkServer.containsCall(ConnCall.CONNECT));

        chunkServer.calls.clear();
        env.nowMs = secondTimeout;
        moreChunks.onTick();
        assertTrue("third reconnect attempt should happen after double timeout",
                chunkServer.containsCall(ConnCall.CONNECT));

        chunkServer.calls.clear();
        moreChunks.onChunkServerConnected();
        assertTrue("successful connection should not result in reconnect attempt",
                !chunkServer.containsCall(ConnCall.CONNECT));

        moreChunks.onChunkServerDisconnected(new DisconnectReason("Test"));
        assertTrue("successful connection should reset reconnect timeout",
                chunkServer.containsCall(ConnCall.CONNECT));

        final long firstTimeoutPart2 = secondTimeout + firstTimeout;

        chunkServer.calls.clear();
        env.nowMs = firstTimeoutPart2 - 1;
        moreChunks.onTick();
        assertTrue("second reconnect attempt should not happen before first timeout (after a successful connection had been made)",
                !chunkServer.containsCall(ConnCall.CONNECT));

        chunkServer.calls.clear();
        env.nowMs = firstTimeoutPart2;
        moreChunks.onTick();
        assertTrue("successful connection should reset reconnect interval",
                chunkServer.containsCall(ConnCall.CONNECT));
    }

    // TODO test when not enabled

    // TODO cap out reconnect time at 60sec
}
