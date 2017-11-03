package gjum.minecraft.forge.morechunks;

import java.util.Collection;

public interface IChunkServerConnection {
    void connect();

    void disconnect(DisconnectReason reason);

    boolean isConnected();

    void requestChunks(Collection<Pos2> chunksPos);

    void sendChunk(Chunk chunk);
}
