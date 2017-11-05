package gjum.minecraft.forge.morechunks;

import java.util.Collection;

public class MockChunkServer extends CallTracker<MockChunkServer.ChunkServerCall> implements IChunkServer {
    boolean connected = false;

    enum ChunkServerCall {IS_CONNECTED, CONNECT, DISCONNECT, REQUEST_CHUNKS, SEND_CHUNK}

    @Override
    public boolean isConnected() {
        trackCall(ChunkServerCall.IS_CONNECTED);
        return connected;
    }

    @Override
    public void connect() {
        trackCall(ChunkServerCall.CONNECT);
    }

    @Override
    public void disconnect(DisconnectReason reason) {
        trackCall(ChunkServerCall.DISCONNECT, reason);
    }

    @Override
    public void requestChunks(Collection<Pos2> chunksPos) {
        trackCall(ChunkServerCall.REQUEST_CHUNKS, chunksPos);
    }

    @Override
    public void sendChunk(Chunk chunk) {
        trackCall(ChunkServerCall.SEND_CHUNK, chunk);
    }
}
