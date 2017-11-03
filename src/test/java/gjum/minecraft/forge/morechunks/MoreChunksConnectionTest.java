package gjum.minecraft.forge.morechunks;

import gjum.minecraft.forge.morechunks.MockChunkServerConnection.ConnCall;
import junit.framework.TestCase;

public class MoreChunksConnectionTest extends TestCase {

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

    public void testConnectsChunkServerOnJoinGame() {
        conn.connected = false;
        game.ingame = true;
        moreChunks.onGameConnected();
        assertTrue(conn.containsCall(ConnCall.CONNECT));
    }

    public void testDisconnectsChunkServerOnLeaveGame() {
        conn.connected = true;
        moreChunks.onGameDisconnected();
        assertEquals(ConnCall.DISCONNECT, conn.getLastCall().call);
        assertEquals("Manual disconnect: Game ending", ((DisconnectReason) conn.getLastCall().args[0]).description);
    }

    public void testReconnectsOnChunkServerDisconnectWhenIngame() {
        conn.connected = false;
        game.ingame = true;
        moreChunks.onChunkServerDisconnected();
        assertEquals(ConnCall.CONNECT, conn.getLastCall().call);
    }

    public void testNoReconnectionOnChunkServerDisconnectWhenNotIngame() {
        conn.connected = false;
        game.ingame = false;
        moreChunks.onChunkServerDisconnected();
        assertFalse(conn.containsCall(ConnCall.CONNECT));
    }

    public void testDisconnectsChunkServerOnConnectWhenNotIngame() {
        conn.connected = true;
        game.ingame = false;
        moreChunks.onChunkServerConnected();
        assertEquals(ConnCall.DISCONNECT, conn.getLastCall().call);
        assertEquals("Manual disconnect: No game running", ((DisconnectReason) conn.getLastCall().args[0]).description);
    }

    public void testNoReconnectOnConnectChunkServerWhenNotIngame() {
        conn.connected = true;
        game.ingame = true;
        moreChunks.onChunkServerConnected();
        assertTrue(conn.calls.isEmpty());
    }

    public void testReconnectWithExponentialBackoff() {
        game.ingame = true;
        conn.connected = false;

        env.nowMs = 0;
        moreChunks.onChunkServerDisconnected();
        assertTrue("first reconnect attempt should happen instantly",
                conn.containsCall(ConnCall.CONNECT));

        conn.calls.clear();
        env.nowMs = 1;
        moreChunks.onTick();
        assertTrue("should not reconnect while waiting for timeout",
                !conn.containsCall(ConnCall.CONNECT));

        moreChunks.onChunkServerDisconnected();
        assertTrue("second reconnect attempt should not happen instantly",
                !conn.containsCall(ConnCall.CONNECT));

        final long firstTimeout = 1000;

        conn.calls.clear();
        env.nowMs = firstTimeout - 1;
        moreChunks.onTick();
        assertTrue("second reconnect attempt should not happen before timeout",
                !conn.containsCall(ConnCall.CONNECT));

        conn.calls.clear();
        env.nowMs = firstTimeout;
        moreChunks.onTick();
        assertTrue("second reconnect attempt should happen after timeout",
                conn.containsCall(ConnCall.CONNECT));

        final long secondTimeout = firstTimeout + 2 * firstTimeout;
        conn.calls.clear();
        env.nowMs = secondTimeout - 1;
        moreChunks.onTick();
        assertTrue("third reconnect attempt should not happen before double timeout",
                !conn.containsCall(ConnCall.CONNECT));

        conn.calls.clear();
        env.nowMs = secondTimeout;
        moreChunks.onTick();
        assertTrue("third reconnect attempt should happen after double timeout",
                conn.containsCall(ConnCall.CONNECT));

        conn.calls.clear();
        moreChunks.onChunkServerConnected();
        assertTrue("successful connection should not result in reconnect attempt",
                !conn.containsCall(ConnCall.CONNECT));

        moreChunks.onChunkServerDisconnected();
        assertTrue("successful connection should reset reconnect timeout",
                conn.containsCall(ConnCall.CONNECT));

        final long firstTimeoutPart2 = secondTimeout + firstTimeout;

        conn.calls.clear();
        env.nowMs = firstTimeoutPart2 - 1;
        moreChunks.onTick();
        assertTrue("second reconnect attempt should not happen before first timeout (after a successful connection had been made)",
                !conn.containsCall(ConnCall.CONNECT));

        conn.calls.clear();
        env.nowMs = firstTimeoutPart2;
        moreChunks.onTick();
        assertTrue("successful connection should reset reconnect interval",
                conn.containsCall(ConnCall.CONNECT));

    }
}
