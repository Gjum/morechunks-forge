package gjum.minecraft.forge.morechunks;

import java.util.Collection;

public interface IChunkServer {

    /**
     * Connect to the chunk server. On connection, calls MoreChunks so it can send initial information.
     */
    void connect(String chunkServerAddress, IMoreChunks moreChunks);

    void disconnect(DisconnectReason reason);

    String getConnectionInfo();

    boolean isConnected();

    /**
     * Send a preference list to the chunk server.
     * It will then send those chunks to the client
     * in that order, skipping any unknown chunks.
     */
    void sendChunksRequest(Collection<Pos2> chunksPos);

    /**
     * Send the chunk to the chunk server, to share it with other players.
     */
    void sendChunk(Chunk chunk);

    void sendChunkLoadsPerSecond(int chunkLoadsPerSecond);

    void sendPlayerDimension(int dim);

    void sendStringMessage(String message);
}
