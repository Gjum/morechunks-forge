package gjum.minecraft.forge.morechunks;

import gjum.minecraft.forge.morechunks.MockChunkServerConnection.ConnCall;
import junit.framework.TestCase;

public class MoreChunksConnectionTest extends TestCase {

    private MoreChunks moreChunks;
    private MockMcGame game;
    private MockChunkServerConnection conn;
    private IConfig conf;

    public void setUp() throws Exception {
        super.setUp();
        game = new MockMcGame();
        conn = new MockChunkServerConnection();
        conf = new MoreChunksConfig();
        moreChunks = new MoreChunks(game, conn, conf);
    }

    public void testConnectsChunkServerOnJoinGame() {
        conn.connected = false;
        moreChunks.onGameConnected();
        assertEquals(ConnCall.CONNECT, conn.getLastCall().call);
    }

    public void testDisconnectsChunkServerOnLeaveGame() {
        conn.connected = true;
        moreChunks.onGameDisconnected();
        assertEquals(ConnCall.DISCONNECT, conn.getLastCall().call);
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
    }

    public void testNoReconnectOnConnectChunkServerWhenNotIngame() {
        conn.connected = true;
        game.ingame = true;
        moreChunks.onChunkServerConnected();
        assertTrue(conn.calls.isEmpty());
    }
}
