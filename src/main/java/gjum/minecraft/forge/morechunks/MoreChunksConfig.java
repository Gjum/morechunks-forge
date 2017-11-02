package gjum.minecraft.forge.morechunks;

public class MoreChunksConfig implements IConfig {
    @Override
    public void blacklistCircle(Pos2 center, int radius) {
        // TODO unimplemented method stub
    }

    @Override
    public void blacklistRectangle(Pos2 corner1, Pos2 corner2) {
        // TODO unimplemented method stub
    }

    @Override
    public boolean canPublishChunk(Pos2 chunkPos) {
        return true; // TODO allow blacklisting chunks
    }
}
