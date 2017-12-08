package gjum.minecraft.forge.morechunks;

import gjum.minecraft.forge.morechunks.MockChunkServer.ChunkServerCall;
import junit.framework.TestCase;

import static gjum.minecraft.forge.morechunks.MockMcGame.MC_ADDRESS;

public class MoreChunksConnectionTest extends TestCase {
    private MoreChunks moreChunks;
    private MockMcGame game;
    private MockChunkServer chunkServer;
    private McServerConfig mcServerConfig;
    private Config conf;
    private MockEnv env;

    public void setUp() throws Exception {
        super.setUp();
        env = new MockEnv();
        game = new MockMcGame(env);
        chunkServer = new MockChunkServer();
        mcServerConfig = new McServerConfig(MC_ADDRESS, true, MockChunkServer.ADDRESS, 5);
        conf = new Config();
        conf.putMcServerConfig(mcServerConfig);
        moreChunks = new MoreChunks(game, conf, env, chunkServer, MoreChunksMod.VERSION);
    }

    public void testConnectsChunkServerOnJoinGame() {
        chunkServer.connected = false;
        game.currentServerIp = MC_ADDRESS;
        game.isPacketHandlerInPipe = false;

        moreChunks.onGameConnected();

        assertTrue(chunkServer.containsCall(ChunkServerCall.CONNECT));
    }

    public void testDisconnectsChunkServerOnLeaveGame() {
        chunkServer.connected = true;
        game.currentServerIp = MC_ADDRESS; // by the nature of Forge's disconnect event, game is still connected at that time
        moreChunks.onGameDisconnected();
        assertEquals("Should disconnect chunk server when leaving game", ChunkServerCall.DISCONNECT, chunkServer.getLastCall().call);
        ExpectedDisconnect reason = new ExpectedDisconnect("MoreChunks: Game ending");
        assertEquals(reason, chunkServer.getLastCall().args[0]);

        chunkServer.connected = false;
        moreChunks.onChunkServerDisconnected(reason);
        assertTrue("Should not reconnect chunk server when leaving game",
                !chunkServer.containsCall(ChunkServerCall.CONNECT));
    }

    public void testReconnectsOnChunkServerDisconnectWhenIngame() {
        chunkServer.connected = false;
        game.currentServerIp = MC_ADDRESS;
        moreChunks.onChunkServerDisconnected(new DisconnectReason("Test"));
        assertEquals(ChunkServerCall.CONNECT, chunkServer.getLastCall().call);
    }

    public void testNoReconnectionOnChunkServerDisconnectWhenNotIngame() {
        chunkServer.connected = false;
        game.currentServerIp = null;
        moreChunks.onChunkServerDisconnected(new DisconnectReason("Test"));
        assertFalse(chunkServer.containsCall(ChunkServerCall.CONNECT));
    }

    public void testDisconnectsChunkServerOnConnectWhenNotIngame() {
        chunkServer.connected = true;
        game.currentServerIp = null;
        moreChunks.onChunkServerConnected();
        assertEquals(ChunkServerCall.DISCONNECT, chunkServer.getLastCall().call);
        ExpectedDisconnect reason = new ExpectedDisconnect("MoreChunks: No game running");
        assertEquals(reason, chunkServer.getLastCall().args[0]);
    }

    public void testNoReconnectOnConnectChunkServerWhenNotIngame() {
        chunkServer.connected = true;
        game.currentServerIp = MC_ADDRESS;
        moreChunks.onChunkServerConnected();

        assertTrue("Should not reconnect ChunkServer when not ingame",
                chunkServer.calls.stream().noneMatch(snap ->
                        snap.call == ChunkServerCall.CONNECT));
    }

    public void testReconnectWithExponentialBackoff() {
        game.currentServerIp = MC_ADDRESS;
        chunkServer.connected = false;

        env.nowMs = 0;
        moreChunks.onChunkServerDisconnected(new DisconnectReason("Test"));
        assertTrue("first reconnect attempt should happen instantly",
                chunkServer.containsCall(ChunkServerCall.CONNECT));

        chunkServer.calls.clear();
        env.nowMs = 1;
        moreChunks.onTick();
        assertTrue("should not reconnect while waiting for timeout",
                !chunkServer.containsCall(ChunkServerCall.CONNECT));

        moreChunks.onChunkServerDisconnected(new DisconnectReason("Test"));
        assertTrue("second reconnect attempt should not happen instantly",
                !chunkServer.containsCall(ChunkServerCall.CONNECT));

        final long firstTimeout = 1000;

        chunkServer.calls.clear();
        env.nowMs = firstTimeout - 1;
        moreChunks.onTick();
        assertTrue("second reconnect attempt should not happen before timeout",
                !chunkServer.containsCall(ChunkServerCall.CONNECT));

        chunkServer.calls.clear();
        env.nowMs = firstTimeout;
        moreChunks.onTick();
        assertTrue("second reconnect attempt should happen after timeout",
                chunkServer.containsCall(ChunkServerCall.CONNECT));

        final long secondTimeout = firstTimeout + 2 * firstTimeout;
        chunkServer.calls.clear();
        env.nowMs = secondTimeout - 1;
        moreChunks.onTick();
        assertTrue("third reconnect attempt should not happen before double timeout",
                !chunkServer.containsCall(ChunkServerCall.CONNECT));

        chunkServer.calls.clear();
        env.nowMs = secondTimeout;
        moreChunks.onTick();
        assertTrue("third reconnect attempt should happen after double timeout",
                chunkServer.containsCall(ChunkServerCall.CONNECT));

        chunkServer.calls.clear();
        moreChunks.onChunkServerConnected();
        assertTrue("successful connection should not result in reconnect attempt",
                !chunkServer.containsCall(ChunkServerCall.CONNECT));

        moreChunks.onChunkServerDisconnected(new DisconnectReason("Test"));
        assertTrue("successful connection should reset reconnect timeout",
                chunkServer.containsCall(ChunkServerCall.CONNECT));

        final long firstTimeoutPart2 = secondTimeout + firstTimeout;

        chunkServer.calls.clear();
        env.nowMs = firstTimeoutPart2 - 1;
        moreChunks.onTick();
        assertTrue("second reconnect attempt should not happen before first timeout (after a successful connection had been made)",
                !chunkServer.containsCall(ChunkServerCall.CONNECT));

        chunkServer.calls.clear();
        env.nowMs = firstTimeoutPart2;
        moreChunks.onTick();
        assertTrue("successful connection should reset reconnect interval",
                chunkServer.containsCall(ChunkServerCall.CONNECT));
    }

    public void testSendsChunkSpeedWhenChangedInConfig() {
        chunkServer.connected = true;
        conf.setChunkLoadsPerSecond(42);

        moreChunks.onConfigChanged();

        assertTrue("Should send new chunk speed to server when it changed in the config",
                chunkServer.containsCall(snap ->
                        snap.call == ChunkServerCall.SEND_CHUNK_LOADS_PER_SEC
                                && snap.args[0].equals(42)));
    }

    public void testSendsNoChunkSpeedWhenNotChangedInConfig() {
        moreChunks.onConfigChanged();

        assertTrue("Should not send chunk speed to server when it didn't change",
                !chunkServer.containsCall(ChunkServerCall.SEND_STRING_MSG));
    }

    // TODO test when not enabled

    // TODO cap out reconnect time at 60sec
}
