package gjum.minecraft.forge.morechunks;

public class MockChunkServerConnection extends CallTracker<MockChunkServerConnection.ConnCall> implements IChunkServerConnection {
    boolean connected = false;

    enum ConnCall {IS_CONNECTED, CONNECT, DISCONNECT, REQUEST_CHUNK, SEND_CHUNK}

    @Override
    public boolean isConnected() {
        trackCall(ConnCall.IS_CONNECTED);
        return connected;
    }

    @Override
    public void connect() {
        trackCall(ConnCall.CONNECT);
    }

    @Override
    public void disconnect() {
        trackCall(ConnCall.DISCONNECT);
    }

    @Override
    public void requestChunk(Pos2 chunkPos) {
        trackCall(ConnCall.REQUEST_CHUNK, chunkPos);
    }

    @Override
    public void sendChunk(Chunk chunk) {
        trackCall(ConnCall.SEND_CHUNK, chunk);
    }
}
