package gjum.minecraft.forge.morechunks;

import java.util.Collection;

public class MockChunkServerConnection extends CallTracker<MockChunkServerConnection.ConnCall> implements IChunkServerConnection {
    boolean connected = false;

    enum ConnCall {IS_CONNECTED, CONNECT, DISCONNECT, REQUEST_CHUNKS, SEND_CHUNK}

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
    public void disconnect(DisconnectReason reason) {
        trackCall(ConnCall.DISCONNECT, reason);
    }

    @Override
    public void requestChunks(Collection<Pos2> chunksPos) {
        trackCall(ConnCall.REQUEST_CHUNKS, chunksPos);
    }

    @Override
    public void sendChunk(Chunk chunk) {
        trackCall(ConnCall.SEND_CHUNK, chunk);
    }
}
