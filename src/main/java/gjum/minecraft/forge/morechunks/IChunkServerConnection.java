package gjum.minecraft.forge.morechunks;

public interface IChunkServerConnection {
    void connect();

    void disconnect();

    boolean isConnected();

    void requestChunk(Pos2 chunkPos);

    void sendChunk(Chunk chunk);
}
