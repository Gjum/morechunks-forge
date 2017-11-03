package gjum.minecraft.forge.morechunks;

public class MoreChunks implements IMoreChunks {
    private final IMcGame game;
    private final IChunkServerConnection conn;
    private final IConfig conf;
    private final IEnv env;

    public MoreChunks(IMcGame game, IChunkServerConnection conn, IConfig conf, IEnv env) {
        this.game = game;
        this.conn = conn;
        this.conf = conf;
        this.env = env;
    }

    @Override
    public void onChunkServerConnected() {
        // TODO reset timeout
        if (!game.isIngame()) {
            conn.disconnect(new DisconnectReason("Manual disconnect: No game running"));
        }
    }

    @Override
    public void onChunkServerDisconnected() {
        if (conn.isConnected()) return;
        // TODO just start the timeout, the actual connection happens in onTick
        if (game.isIngame()) {
            conn.connect();
        }
    }

    @Override
    public void onGameConnected() {
        if (!conn.isConnected()) {
            conn.connect();
        }
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
        if (conn.isConnected()) {
            checkLoadExtraChunks();
        } else {
            checkRetryConnectChunkServer();
        }
    }

    private void checkRetryConnectChunkServer() {
        final long now = env.currentTimeMillis();
        // TODO check if last reconnect failure is longer ago than current exponential backoff time
    }

    private void checkLoadExtraChunks() {
        final long now = env.currentTimeMillis();
        // TODO check if last game-chunk-load is longer ago than expected time between chunk loads
    }
}
