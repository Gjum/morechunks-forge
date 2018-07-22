package gjum.minecraft.forge.morechunks;

import org.apache.logging.log4j.Level;

import java.util.*;
import java.util.stream.Collectors;

public class MoreChunks implements IMoreChunks {
    private final IMcGame game;
    private final IEnv env;
    private final Config config;
    private final String versionStr;

    public final IChunkServer chunkServer;

    private Pos2 lastRequestPlayerPos = new Pos2(Integer.MAX_VALUE, Integer.MAX_VALUE);

    private int serverRenderDistance; // cached because it's used a lot (e.g. on every incoming chunk)

    private long nextReconnectTime = 0;
    private long nextRetryInterval = 1000;

    /**
     * When the game sends us a chunk, but we're not connected to the ChunkServer yet,
     * we store the chunk and send it once the ChunkServer is connected.
     */
    private ArrayList<Chunk> chunksToSendWhenConnected = new ArrayList<>();

    public MoreChunks(IMcGame game, Config config, IEnv env, IChunkServer chunkServer, String versionStr) {
        this.game = game;
        this.config = config;
        this.env = env;
        this.versionStr = versionStr;
        this.chunkServer = chunkServer;

        onConfigChanged();
    }

    @Override
    public int decideUndergroundCutOff(ChunkData chunkData) {
        final int SECTION_HEIGHT = 16;
        final int CHUNK_SECTIONS = 16;

        // TODO decide underground cutoff from config (min/max in-/exclusion heights)
        final int CUT_OFF_PERCENTAGE = 100;

        int colsInChunk = 256;
        int[] colsInSection = new int[CHUNK_SECTIONS]; // number of columns which have their top opaque block in that section
        for (int highestOpaqueBlock : chunkData.heightMap) {
            if (highestOpaqueBlock == 0) {
                --colsInChunk;
                continue;
            }
            final int sectionNr = (highestOpaqueBlock - 1) / SECTION_HEIGHT;
            colsInSection[sectionNr] += 1;
        }

        int colsHereAndUp = 0;
        for (int sectionNr = CHUNK_SECTIONS - 1; sectionNr >= 0; sectionNr--) {
            colsHereAndUp += colsInSection[sectionNr];
            final int percentColsContained = 100 * colsHereAndUp / colsInChunk;
            if (percentColsContained >= CUT_OFF_PERCENTAGE) {
                // the sections here and up contain most of the top opaque blocks,
                // so we remove all sections below this one
                return sectionNr - 1;
            }
        }

        throw new RuntimeException("Unreachable: all sections together should contain all blocks");
    }

    @Override
    public void onChunkServerConnected() {
        nextReconnectTime = 0;
        nextRetryInterval = 1000;

        if (!game.isIngame()) {
            chunkServer.disconnect(new ExpectedDisconnect("MoreChunks: No game running"));
            return;
        }

        env.log(Level.INFO, "ChunkServer: Connected to %s", chunkServer.getConnectionInfo());

        chunkServer.sendStringMessage("mod.version=" + versionStr);
        chunkServer.sendStringMessage("game.address=" + game.getCurrentServerIp());
        chunkServer.sendPlayerDimension(game.getPlayerDimension());
        chunkServer.sendChunkLoadsPerSecond(config.getChunkLoadsPerSecond());

        for (Chunk chunk : chunksToSendWhenConnected) {
            chunkServer.sendChunk(chunk);
        }
        chunksToSendWhenConnected.clear();
    }

    @Override
    public void onChunkServerDisconnected(DisconnectReason reason) {
        if (reason instanceof ExpectedDisconnect) return;
        retryConnectChunkServer();
    }

    @Override
    public void onConfigChanged() {
        if (game.isIngame()) {
            serverRenderDistance = config.getMcServerConfig(game.getCurrentServerIp()).serverRenderDistance;
        }

        if (chunkServer.isConnected()) {
            chunkServer.sendChunkLoadsPerSecond(config.getChunkLoadsPerSecond());
        }
    }

    @Override
    public void onGameConnected() {
        env.log(Level.DEBUG, "Connected to game");

        if (game.wasPacketHandlerAlreadyInserted()) {
            env.log(Level.ERROR, "packet handler already inserted");
            return;
        }

        final McServerConfig mcServerConfig = config.getMcServerConfig(game.getCurrentServerIp());
        if (mcServerConfig == null || !mcServerConfig.enabled) {
            if (chunkServer.isConnected()) {
                chunkServer.disconnect(new ExpectedDisconnect("Connecting to disabled game server"));
            }

            return;
        }

        game.insertPacketHandler(this);

        serverRenderDistance = mcServerConfig.serverRenderDistance;

        retryConnectChunkServer();
    }

    @Override
    public void onGameDisconnected() {
        env.log(Level.DEBUG, "Game disconnected");
        if (chunkServer.isConnected()) {
            chunkServer.disconnect(new ExpectedDisconnect("MoreChunks: Game ending"));
        }
        game.clearChunkCache();
        chunksToSendWhenConnected.clear();
    }

    @Override
    public void onPlayerChangedDimension(int toDim) {
        env.log(Level.DEBUG, "Player moved to dimension %s", toDim);
        // TODO test
        if (chunkServer.isConnected()) chunkServer.sendPlayerDimension(toDim);
        else retryConnectChunkServer();
        game.clearChunkCache();
        chunksToSendWhenConnected.clear();
    }

    @Override
    public void onReceiveExtraChunk(Chunk chunk) {
        if (!game.isIngame()) {
            env.log(Level.WARN, "Received extra chunk at %s while not ingame", chunk.pos);
            return;
        }

        final int chunkDistance = 1 + chunk.pos.chebyshevDistance(game.getPlayerChunkPos());

        if (chunkDistance > game.getRenderDistance()) {
            env.log(Level.DEBUG, "Discarding too far extra chunk at %s", chunk.pos);
            return;
        }

        if (chunkDistance <= serverRenderDistance) {
            env.log(Level.DEBUG, "Discarding too close extra chunk at %s", chunk.pos);
            return;
        }

        game.runOnMcThread(() -> {
            if (game.getLoadedChunks().contains(chunk.pos)) {
                env.log(Level.DEBUG, "Discarding already loaded extra chunk at %s", chunk.pos);
                return;
            }

            game.unloadChunk(chunk.pos);
            game.loadChunk(chunk);
            unloadChunksOverCap();
        });
    }

    @Override
    public void onReceiveGameChunk(Chunk chunk) {
        if (chunkServer.isConnected()) {
            chunkServer.sendChunk(chunk);
        } else {
            if (chunksToSendWhenConnected.size() < 200) {
                chunksToSendWhenConnected.add(chunk);
                if (chunksToSendWhenConnected.size() == 200) {
                    env.log(Level.WARN, "Chunk Server still not connected when receiving 200th game chunk");
                }
            }
        }
        game.runOnMcThread(() -> {
            game.unloadChunk(chunk.pos);
            game.loadChunk(chunk);

            if (chunkServer.isConnected()) {
                requestExtraChunks();
            }
            unloadChunksOutsideRenderDistance();
            unloadChunksOverCap();
        });
    }

    @Override
    public void onStatusMsg(String statusMsg) {
        if (statusMsg.charAt(0) == 'i') {
            int positionBits = statusMsg.charAt(1) - '0';
            String msg = statusMsg.substring(3);

            if ((positionBits & 0b1) != 0) {
                if (game.isIngame()) game.showChat(msg);
            }
            if ((positionBits & 0b10) != 0) {
                if (game.isIngame()) game.showAchievement(MoreChunksMod.MOD_NAME, msg);
            }
            if ((positionBits & 0b100) != 0) {
                if (game.isIngame()) game.showHotbarMsg(msg);
            }

        } else if (statusMsg.charAt(0) == '!') {
            if (statusMsg.startsWith("! serverRenderDistance=")) {
                serverRenderDistance = Integer.parseUnsignedInt(statusMsg.substring(statusMsg.indexOf("=") + 1));

            } else if (statusMsg.startsWith("! kick ms=")) {
                final long now = env.currentTimeMillis();
                final int kickTimeout = Integer.parseUnsignedInt(statusMsg.substring(statusMsg.indexOf("=") + 1));
                nextReconnectTime = now + kickTimeout;
                chunkServer.disconnect(new ExpectedDisconnect("MoreChunks: Kicked by chunk server"));
            }
        }
    }

    @Override
    public void onTick() {
        retryConnectChunkServer();
    }

    private void requestExtraChunks() {
        if (!chunkServer.isConnected()) return;

        final Pos2 playerChunkPos = game.getPlayerChunkPos();
        if (playerChunkPos.equals(lastRequestPlayerPos)) return; // TODO test

        List<Pos2> loadableChunks = getLoadableChunks();

        // apply limit
        // TODO sort loadable chunks by interest instead (e.g. within player's walking direction)
        loadableChunks = sortByPlayerDistance(loadableChunks);
        final int chunkLoadLimit = config.getMaxNumChunksLoaded() - game.getLoadedChunks().size();
        if (chunkLoadLimit <= 0) return; // TODO test
        loadableChunks = loadableChunks.stream().limit(chunkLoadLimit).collect(Collectors.toList());

        lastRequestPlayerPos = playerChunkPos;

        chunkServer.sendChunksRequest(loadableChunks);
    }

    /**
     * Build a list of all chunk positions inside the client's render distance
     * that are neither loaded nor within server's' render distance.
     */
    private List<Pos2> getLoadableChunks() {
        final int rdClient = game.getRenderDistance() - 1;
        final Collection<Pos2> loadedChunks = game.getLoadedChunks();
        final Pos2 player = game.getPlayerChunkPos();

        final List<Pos2> loadable = new ArrayList<>();
        for (int x = player.x - rdClient; x <= player.x + rdClient; x++) {
            for (int z = player.z - rdClient; z <= player.z + rdClient; z++) {
                Pos2 chunk = new Pos2(x, z);

                if (1 + player.chebyshevDistance(chunk) <= serverRenderDistance) {
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
        chunks.sort(Comparator.comparingDouble(player::taxicabDistance));
        return chunks;
    }

    private void retryConnectChunkServer() {
        if (chunkServer.isConnected()) return;
        if (!game.isIngame()) return;

        final String currentServerIp = game.getCurrentServerIp();
        if (currentServerIp == null) {
            env.log(Level.ERROR, "isIngame but currentServerIp == null");
            return;
        }

        final long now = env.currentTimeMillis();
        if (nextReconnectTime > now) return; // onTick() will retry on timeout

        nextReconnectTime = now + nextRetryInterval;
        nextRetryInterval *= 2;
        if (nextRetryInterval > 6000) nextRetryInterval = 6000; // TODO test

        final McServerConfig mcServerConfig = config.getMcServerConfig(currentServerIp);
        if (mcServerConfig == null) {
            return;
        }

        chunkServer.connect(mcServerConfig.chunkServerAddress, this);
    }

    private void unloadChunksOutsideRenderDistance() {
        final Pos2 player = game.getPlayerChunkPos();
        int renderDistance = game.getRenderDistance();
        ArrayList<Pos2> chunksToUnload = new ArrayList<>();
        final long oneSecondAgo = env.currentTimeMillis() - 1000;
        for (Map.Entry<Pos2, Long> posWithTime : game.getChunkLoadTimes().entrySet()) {
            if (1 + player.chebyshevDistance(posWithTime.getKey()) <= renderDistance) continue;
            if (oneSecondAgo < posWithTime.getValue()) continue;
            chunksToUnload.add(posWithTime.getKey());
        }

        if (chunksToUnload.isEmpty()) {
            return;
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
            final Pos2 player = game.getPlayerChunkPos();
            final PriorityQueue<Map.Entry<Pos2, Long>> closeChunks = new PriorityQueue<>(Comparator.comparingInt(
                    posWithTime -> player.chebyshevDistance(posWithTime.getKey())));
            closeChunks.addAll(game.getChunkLoadTimes().entrySet());
            final long oneSecondAgo = env.currentTimeMillis() - 1000;
            closeChunks.stream()
                    .filter(posWithTime -> oneSecondAgo > posWithTime.getValue())
                    .skip(config.getMaxNumChunksLoaded())
                    .forEach(posWithTime -> game.unloadChunk(posWithTime.getKey()));
        }
    }
}
