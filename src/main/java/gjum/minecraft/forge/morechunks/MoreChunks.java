package gjum.minecraft.forge.morechunks;

public class MoreChunks implements IMoreChunks {
    private final IMcGame game;
    private final IChunkServerConnection conn;
    private final IConfig conf;
    private final IEnv env;
    private long nextReconnectTime = 0;
    private long nextRetryInterval = 1000;

    public MoreChunks(IMcGame game, IChunkServerConnection conn, IConfig conf, IEnv env) {
        this.game = game;
        this.conn = conn;
        this.conf = conf;
        this.env = env;
    }

    @Override
    public void onChunkServerConnected() {
        nextReconnectTime = env.currentTimeMillis();
        nextRetryInterval = 1000;
        if (!game.isIngame()) {
            conn.disconnect(new DisconnectReason("Manual disconnect: No game running"));
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
        if (conn.isConnected()) {
            conn.disconnect(new DisconnectReason("Manual disconnect: Game ending"));
        }
    }

    @Override
    public void onReceiveExtraChunk(Chunk chunk) {
        if (!game.isIngame()) return;
        final int chunkDistance = chunk.pos.maxisDistance(game.getPlayerChunkPos());
        if (chunkDistance > game.getRenderDistance()) return;
        // TODO ignore if there's already a game chunk at that pos
        game.loadChunk(chunk);
        // TODO track as extra chunk
    }

    @Override
    public void onReceiveGameChunk(Chunk chunk) {
        // TODO auto-generated method stub
    }

    @Override
    public void onTick() {
        if (!conn.isConnected()) {
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
        if (conn.isConnected()) return;
        if (!game.isIngame()) return;

        final long now = env.currentTimeMillis();
        if (nextReconnectTime > now) return; // onTick() will recheck on timeout

        nextReconnectTime = now + nextRetryInterval;
        nextRetryInterval *= 2;

        conn.connect();
    }
}
