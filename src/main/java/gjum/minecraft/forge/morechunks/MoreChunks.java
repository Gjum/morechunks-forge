package gjum.minecraft.forge.morechunks;

import java.util.*;

public class MoreChunks implements IMoreChunks {
    private final IMcGame game;
    private final IChunkServerConnection chunkServer;
    private final IConfig conf;
    private final IEnv env;
    private long nextReconnectTime = 0;
    private long nextRetryInterval = 1000;
    private int serverRenderDistance = 4;

    public MoreChunks(IMcGame game, IChunkServerConnection chunkServer, IConfig conf, IEnv env) {
        this.game = game;
        this.chunkServer = chunkServer;
        this.conf = conf;
        this.env = env;
    }

    @Override
    public void onChunkServerConnected() {
        nextReconnectTime = env.currentTimeMillis();
        nextRetryInterval = 1000;
        if (!game.isIngame()) {
            chunkServer.disconnect(new DisconnectReason("Manual disconnect: No game running"));
        }
    }

    @Override
    public void onChunkServerDisconnected() {
        retryConnectChunkServer();
    }

    @Override
    public void onGameConnected() {
        retryConnectChunkServer();
    }

    @Override
    public void onGameDisconnected() {
        if (chunkServer.isConnected()) {
            chunkServer.disconnect(new DisconnectReason("Manual disconnect: Game ending"));
        }
    }

    @Override
    public void onReceiveExtraChunk(Chunk chunk) {
        if (!game.isIngame()) return;

        final int chunkDistance = chunk.pos.maxisDistance(game.getPlayerChunkPos());
        if (chunkDistance > game.getRenderDistance()) return;

        if (game.getLoadedChunks().contains(chunk.pos)) return;
        // TODO only ignore if loaded chunk is game chunk, to allow updating with more recent extra chunks

        game.loadChunk(chunk);
    }

    @Override
    public void onReceiveGameChunk(Chunk chunk) {
        if (chunkServer.isConnected()) {
            chunkServer.sendChunk(chunk);
        }
        unloadChunksOutsideRenderDistance();
        requestExtraChunks();
    }

    @Override
    public void onTick() {
        if (!chunkServer.isConnected()) {
            retryConnectChunkServer();
            return;
        }
    }

    private void requestExtraChunks() {
        if (!chunkServer.isConnected()) return;
        List<Pos2> loadableChunks = getLoadableChunks();
        chunkServer.requestChunks(loadableChunks);
    }

    /**
     * Build a list of all chunk positions inside the client's render distance
     * that are neither loaded nor within server's' render distance.
     */
    private List<Pos2> getLoadableChunks() {
        final int rd = game.getRenderDistance();
        final Pos2 p = game.getPlayerChunkPos();
        final Collection<Pos2> loadedChunks = game.getLoadedChunks();
        final List<Pos2> loadable = new ArrayList<>();
        for (int x = p.x - rd; x <= p.x + rd; x++) {
            for (int z = p.z - rd; z <= p.z + rd; z++) {
                Pos2 pos = new Pos2(x, z);
                if (p.maxisDistance(pos) <= serverRenderDistance) {
                    continue;
                }
                // TODO inefficient? measure performance
                if (!loadedChunks.contains(pos)) {
                    loadable.add(pos);
                }
            }
        }
        return loadable;
    }

    private void retryConnectChunkServer() {
        if (chunkServer.isConnected()) return;
        if (!game.isIngame()) return;

        final long now = env.currentTimeMillis();
        if (nextReconnectTime > now) return; // onTick() will recheck on timeout

        nextReconnectTime = now + nextRetryInterval;
        nextRetryInterval *= 2;

        chunkServer.connect();
    }

    private void unloadChunksOutsideRenderDistance() {
        final Pos2 player = game.getPlayerChunkPos();
        int renderDistance = game.getRenderDistance();
        ArrayList<Pos2> chunksToUnload = new ArrayList<>();
        for (Pos2 chunkPos : game.getLoadedChunks()) {
            if (player.maxisDistance(chunkPos) > renderDistance) {
                chunksToUnload.add(chunkPos);
            }
        }
        for (Pos2 chunkPos : chunksToUnload) {
            game.unloadChunk(chunkPos);
        }
    }
}
