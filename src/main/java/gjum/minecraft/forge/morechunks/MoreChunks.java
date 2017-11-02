package gjum.minecraft.forge.morechunks;

public class MoreChunks implements IMoreChunks {
    private final IMcGame game;
    private final IChunkServerConnection conn;
    private final IConfig conf;

    public MoreChunks(IMcGame game, IChunkServerConnection conn, IConfig conf) {
        this.game = game;
        this.conn = conn;
        this.conf = conf;
    }

    @Override
    public void onChunkServerConnected() {
        if (!game.isIngame()) {
            conn.disconnect();
        }
    }

    @Override
    public void onChunkServerDisconnected() {
        if (conn.isConnected()) return;
        // TODO timeout
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
            conn.disconnect();
        }
    }

    @Override
    public void onReceiveExtraChunk(Chunk chunk) {
        if (!game.isIngame()) return;
        final int chunkDistance = chunk.pos.maxisDistance(game.getPlayerChunkPos());
        if (chunkDistance > game.getRenderDistance()) return;
        game.loadChunk(chunk);
    }

    @Override
    public void onReceiveGameChunk(Chunk chunk) {
        // TODO auto-generated method stub
    }

    @Override
    public void onTick(long timeMs) {
        // TODO auto-generated method stub
    }
}
