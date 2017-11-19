package gjum.minecraft.forge.morechunks;

public interface IMoreChunks {
    int decideUndergroundCutOff(int[] heightMap);

    void onChunkServerConnected();

    void onChunkServerDisconnected(DisconnectReason reason);

    void onConfigChanged();

    void onGameConnected();

    void onGameDisconnected();

    void onPlayerChangedDimension(int toDim);

    void onReceiveExtraChunk(Chunk chunk);

    void onReceiveGameChunk(Chunk chunk);

    void onTick();
}
