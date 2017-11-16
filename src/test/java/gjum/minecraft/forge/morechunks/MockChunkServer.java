package gjum.minecraft.forge.morechunks;

import java.util.Collection;

public class MockChunkServer extends CallTracker<MockChunkServer.ChunkServerCall> implements IChunkServer {
    static final String ADDRESS = "morechunks.example.com";

    boolean connected = false;

    enum ChunkServerCall {IS_CONNECTED, CONNECT, DISCONNECT, SEND_CHUNKS_REQUEST, SEND_CHUNK, SEND_CHUNK_LOADS_PER_SEC, SEND_PLAYER_DIM, SEND_STRING_MSG}

    @Override
    public void connect(String chunkServerAddress, IMoreChunks moreChunks) {
        trackCall(ChunkServerCall.CONNECT, chunkServerAddress, moreChunks);
    }

    @Override
    public void disconnect(DisconnectReason reason) {
        trackCall(ChunkServerCall.DISCONNECT, reason);
    }

    @Override
    public boolean isConnected() {
        trackCall(ChunkServerCall.IS_CONNECTED);
        return connected;
    }

    @Override
    public void sendChunk(Chunk chunk) {
        trackCall(ChunkServerCall.SEND_CHUNK, chunk);
    }

    @Override
    public void sendChunksRequest(Collection<Pos2> chunksPos) {
        trackCall(ChunkServerCall.SEND_CHUNKS_REQUEST, chunksPos);
    }

    @Override
    public void sendChunkLoadsPerSecond(int chunkLoadsPerSecond) {
        trackCall(ChunkServerCall.SEND_CHUNK_LOADS_PER_SEC, chunkLoadsPerSecond);
    }

    @Override
    public void sendPlayerDimension(int dim) {
        trackCall(ChunkServerCall.SEND_PLAYER_DIM, dim);
    }

    @Override
    public void sendStringMessage(String message) {
        trackCall(ChunkServerCall.SEND_STRING_MSG, message);
    }
}
