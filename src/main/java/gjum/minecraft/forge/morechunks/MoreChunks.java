package gjum.minecraft.forge.morechunks;

import java.util.ArrayList;

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
    }

    @Override
    public void onTick() {
        if (!chunkServer.isConnected()) {
            retryConnectChunkServer();
            return;
        }

        requestExtraChunks();
    }

    private void requestExtraChunks() {
        final long now = env.currentTimeMillis();
        // TODO check if last game-chunk-load is longer ago than expected time between chunk loads
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
