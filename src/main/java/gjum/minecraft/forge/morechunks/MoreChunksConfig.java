package gjum.minecraft.forge.morechunks;

public class MoreChunksConfig implements IConfig {
    private int maxNumChunksLoaded = 16 * 16;
    private int serverRenderDistance = 4;

    // TODO cache blacklist using bitwise filter thing
    // because there will be lots of non-matches, which that data structure is optimized for

    @Override
    public void blacklistCircle(Pos2 center, int radius) {
        // TODO store in source-of-truth, mark blacklist as dirty
    }

    @Override
    public void blacklistRectangle(Pos2 corner1, Pos2 corner2) {
        // TODO store in source-of-truth, mark blacklist as dirty
    }

    @Override
    public boolean canPublishChunk(Pos2 chunkPos) {
        // TODO rebuild blacklist if dirty
        // TODO check against blacklist
        return true;
    }

    @Override
    public int getMaxNumChunksLoaded() {
        return maxNumChunksLoaded;
    }

    @Override
    public int getServerRenderDistance() {
        return serverRenderDistance;
    }

    @Override
    public void setMaxNumChunksLoaded(int maxNumChunksLoaded) {
        this.maxNumChunksLoaded = maxNumChunksLoaded;
    }

    @Override
    public void setServerRenderDistance(int serverRenderDistance) {
        this.serverRenderDistance = serverRenderDistance;
    }
}
