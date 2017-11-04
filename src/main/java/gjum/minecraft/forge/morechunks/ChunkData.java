package gjum.minecraft.forge.morechunks;

import net.minecraft.network.play.server.SPacketChunkData;

import java.io.IOException;

class ChunkData {
    static Chunk convertChunk(SPacketChunkData chunkPacket) throws IOException {
        // TODO remove underground chunk sections here
        Pos2 pos = new Pos2(chunkPacket.getChunkX(), chunkPacket.getChunkZ());
        return new Chunk(pos, chunkPacket);
    }
}
