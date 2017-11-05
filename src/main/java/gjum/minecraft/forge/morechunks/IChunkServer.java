package gjum.minecraft.forge.morechunks;

import java.util.Collection;

public interface IChunkServer {
    /**
     * (Re-)Connect to the chunk server and send initial information
     * about the client, such as world name and chunk sending speed.
     */
    void connect();

    void disconnect(DisconnectReason reason);

    boolean isConnected();

    /**
     * Send a preference list to the chunk server.
     * It will then send those chunks to the client
     * in that order, skipping any unknown chunks.
     */
    void requestChunks(Collection<Pos2> chunksPos);

    /**
     * Send the chunk to the chunk server, to share it with other players.
     */
    void sendChunk(Chunk chunk);

    void sendStringMessage(String message);
}
