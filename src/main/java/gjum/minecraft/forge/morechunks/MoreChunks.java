package gjum.minecraft.forge.morechunks;

import org.apache.logging.log4j.Level;

import java.util.*;
import java.util.stream.Collectors;

import static gjum.minecraft.forge.morechunks.IChunkServer.INFO_SET_CHUNKS_PER_SEC;

public class MoreChunks implements IMoreChunks {
    private final IMcGame game;
    private final IEnv env;

    private IChunkServer chunkServer;
    private IConfig config;
    private Pos2 lastRequestPlayerPos = new Pos2(Integer.MAX_VALUE, Integer.MAX_VALUE);

    private long nextReconnectTime = 0;
    private long nextRetryInterval = 1000;

    public MoreChunks(IMcGame game, IConfig config, IEnv env) {
        this.game = game;
        this.config = config;
        this.env = env;
    }

    public void setChunkServer(IChunkServer chunkServer) {
        this.chunkServer = chunkServer;
    }

    @Override
    public void onChunkServerConnected() {
        nextReconnectTime = 0;
        nextRetryInterval = 1000;

        if (!game.isIngame()) {
            chunkServer.disconnect(new ExpectedDisconnect("MoreChunks: No game running"));
        }
    }

    @Override
    public void onChunkServerDisconnected(DisconnectReason reason) {
        if (reason instanceof ExpectedDisconnect) return;
        env.log(Level.WARN, "ChunkServer disconnected: %s", reason);
        retryConnectChunkServer();
    }

    @Override
    public void onConfigChanged(IConfig newConfig) {
        if (config.getChunkLoadsPerSecond() != newConfig.getChunkLoadsPerSecond()) {
            chunkServer.sendStringMessage(INFO_SET_CHUNKS_PER_SEC + newConfig.getChunkLoadsPerSecond());
        }

        config = newConfig;
    }

    @Override
    public void onGameConnected() {
        env.log(Level.DEBUG, "Connected to game");
        retryConnectChunkServer();
    }

    @Override
    public void onGameDisconnected() {
        env.log(Level.DEBUG, "Game disconnected");
        if (chunkServer.isConnected()) {
            chunkServer.disconnect(new ExpectedDisconnect("MoreChunks: Game ending"));
        }
    }

    @Override
    public void onPlayerChangedDimension(int toDim) {
        env.log(Level.DEBUG, "Player moved to dimension %s", toDim);
        // TODO test
        chunkServer.sendStringMessage("game.dimension=" + toDim);
    }

    @Override
    public void onReceiveExtraChunk(Chunk chunk) {
        if (!game.isIngame()) {
            env.log(Level.WARN, "Received extra chunk at %s while not ingame", chunk.pos);
            return;
        }

        final int chunkDistance = chunk.pos.chebyshevDistance(game.getPlayerChunkPos());

        if (chunkDistance > game.getRenderDistance()) {
            env.log(Level.DEBUG, "Discarding too far extra chunk at %s", chunk.pos);
            return;
        }

        if (chunkDistance <= config.getServerRenderDistance()) {
            env.log(Level.DEBUG, "Discarding too close extra chunk at %s", chunk.pos);
            return;
        }

        game.runOnMcThread(() -> {
            game.unloadChunk(chunk.pos);
            game.loadChunk(chunk);
            unloadChunksOverCap();
        });
    }

    @Override
    public void onReceiveGameChunk(Chunk chunk) {
        if (chunkServer.isConnected()) {
            chunkServer.sendChunk(chunk);
        }
        game.runOnMcThread(() -> {
            game.unloadChunk(chunk.pos);
            game.loadChunk(chunk);
            unloadChunksOutsideRenderDistance();
            requestExtraChunks();
            unloadChunksOverCap();
        });
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
        if (lastRequestPlayerPos.equals(game.getPlayerChunkPos())) return; // TODO test

        List<Pos2> loadableChunks = getLoadableChunks();

        // apply limit
        // TODO sort loadable chunks by interest instead (e.g. within player's walking direction)
        loadableChunks = sortByPlayerDistance(loadableChunks);
        final int chunkLoadLimit = config.getMaxNumChunksLoaded() - game.getLoadedChunks().size();
        if (chunkLoadLimit <= 0) return; // TODO test
        loadableChunks = loadableChunks.stream().limit(chunkLoadLimit).collect(Collectors.toList());

        lastRequestPlayerPos = game.getPlayerChunkPos();

        chunkServer.requestChunks(loadableChunks);
    }

    /**
     * Build a list of all chunk positions inside the client's render distance
     * that are neither loaded nor within server's' render distance.
     */
    private List<Pos2> getLoadableChunks() {
        final int rdClient = game.getRenderDistance();
        final int rdServer = config.getServerRenderDistance();
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

    /**
     * Sort chunk positions by taxicab distance to the player, in-place, ascending.
     *
     * @param chunks chunk positions to sort
     * @return sorted chunk positions
     */
    private List<Pos2> sortByPlayerDistance(List<Pos2> chunks) {
        final Pos2 player = game.getPlayerChunkPos();
        chunks.sort(Comparator.comparingDouble(player::euclidDistanceSq));
        return chunks;
    }

    private void retryConnectChunkServer() {
        if (chunkServer.isConnected()) return;
        if (!game.isIngame()) return;

        final long now = env.currentTimeMillis();
        if (nextReconnectTime > now) return; // onTick() will recheck on timeout

        nextReconnectTime = now + nextRetryInterval;
        nextRetryInterval *= 2;
        if (nextRetryInterval > 6000) nextRetryInterval = 6000; // TODO test

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

    /**
     * Check if too many chunks are loaded, and unload the far away ones.
     */
    private void unloadChunksOverCap() {
        // TODO do not unload extra chunks over cap when only recently loaded, this could prevent flickering
        if (game.getLoadedChunks().size() > config.getMaxNumChunksLoaded()) {
            PriorityQueue<Pos2> closeChunks = new PriorityQueue<>(Comparator.comparingDouble(game.getPlayerChunkPos()::taxicabDistance));
            closeChunks.addAll(game.getLoadedChunks());
            closeChunks.stream()
                    .skip(config.getMaxNumChunksLoaded())
                    .forEach(game::unloadChunk);
        }
    }
}
