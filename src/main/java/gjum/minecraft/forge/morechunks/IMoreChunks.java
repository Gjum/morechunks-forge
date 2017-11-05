package gjum.minecraft.forge.morechunks;

public interface IMoreChunks {
    void onChunkServerConnected();

    void onChunkServerDisconnected(DisconnectReason reason);

    void onGameConnected();

    void onGameDisconnected();

    void onPlayerChangedDimension(int toDim);

    void onReceiveExtraChunk(Chunk chunk);

    void onReceiveGameChunk(Chunk chunk);

    void onTick();
}
