package gjum.minecraft.forge.morechunks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class MoreChunks implements IMoreChunks {
    private final IMcGame game;
    private final IChunkServerConnection chunkServer;
    private final IConfig conf;
    private final IEnv env;
    private long nextReconnectTime = 0;
    private long nextRetryInterval = 1000;

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

        final int chunkDistance = chunk.pos.chebyshevDistance(game.getPlayerChunkPos());
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
        final int rdClient = game.getRenderDistance();
        final int rdServer = conf.getServerRenderDistance();
        final Collection<Pos2> loadedChunks = game.getLoadedChunks();
        final Pos2 player = game.getPlayerChunkPos();

        final List<Pos2> loadable = new ArrayList<>();
        for (int x = player.x - rdClient; x <= player.x + rdClient; x++) {
            for (int z = player.z - rdClient; z <= player.z + rdClient; z++) {
                Pos2 chunk = new Pos2(x, z);

                if (player.chebyshevDistance(chunk) <= rdServer) {
                    // do not load extra chunks inside the server's render distance,
                    // we expect the server to send game chunks here eventually
                    continue;
                }

                // TODO inefficient check? measure performance
                if (!loadedChunks.contains(chunk)) {
                    loadable.add(chunk);
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
            if (player.chebyshevDistance(chunkPos) > renderDistance) {
                chunksToUnload.add(chunkPos);
            }
        }
        for (Pos2 chunkPos : chunksToUnload) {
            game.unloadChunk(chunkPos);
        }
    }
}
