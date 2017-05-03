package gjum.minecraft.forge.chunkfix;

import gjum.minecraft.forge.chunkfix.config.ChunkFixConfig;
import io.netty.channel.ChannelPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collection;

import static org.apache.logging.log4j.Level.ERROR;
import static org.apache.logging.log4j.Level.WARN;

public class ChunkFix {
    private static Minecraft mc = Minecraft.getMinecraft();

    private static Logger logger;

    private MinecraftPacketHandler minecraftPacketHandler;

    private Connection connection;

    private long lastTimeReceivedGameChunk;
    private long lastTimeRequestedExtraChunks;

    public static void setLogger(Logger logger) {
        ChunkFix.logger = logger;
    }

    public static void log(String format, Object... params) {
        log(Level.INFO, format, params);
    }

    public static void log(Level level, String format, Object... params) {
        format = "[" + ChunkFixMod.MOD_NAME + "] " + format.replaceAll("%[a-z]", "%s");
        logger.printf(level, format, params);
    }

    public ChunkFix() {
        connection = new Connection(this);
    }

    /**
     * Inject our {@link MinecraftPacketHandler} into the pipeline.
     * <p>
     * This runs every time the client joins a server, so our {@link MinecraftPacketHandler} has to be tagged Sharable.
     */
    public void onJoinServer() {
        if (!ChunkFixConfig.instance.isEnabled()) return;

        minecraftPacketHandler = null; // XXX HACK to reinject on each connection

        // give the game server 5 seconds to start sending chunks
        lastTimeReceivedGameChunk = System.currentTimeMillis() + 5000;
    }

    public void afterConnectionIsMade() {
        if (minecraftPacketHandler == null) {
            injectPacketHandlerIntoGameServerPipeline();
            connection.connect();
            lastTimeRequestedExtraChunks = System.currentTimeMillis() + 1000;
        }
    }

    public void injectPacketHandlerIntoGameServerPipeline() {
        if (minecraftPacketHandler == null) {
            minecraftPacketHandler = new MinecraftPacketHandler(this);
            ChannelPipeline pipe = mc.getConnection().getNetworkManager().channel().pipeline();
            pipe.addBefore("fml:packet_handler", MinecraftPacketHandler.NAME, minecraftPacketHandler);
        }
    }

    /**
     * When a chunk is received from the game server, send it to the chunk server.
     */
    public void onRegularChunkReceived(SPacketChunkData packet, long timestamp) {
        if (!ChunkFixConfig.instance.isEnabled()) return;

        if (!packet.doChunkLoad()) {
            return; // ignore partial chunks
        }

        lastTimeReceivedGameChunk = System.currentTimeMillis();

        if (ChunkFixConfig.instance.canPublishChunk(packet.getChunkX(), packet.getChunkZ())) {
            try {
                connection.sendChunkPacket(packet, timestamp);
            } catch (IOException e) {
                log(ERROR, "Could not send chunk to chunk server");
                e.printStackTrace();
            }
        }
    }

    /**
     * When a chunk is received from the chunk server, load it into the game.
     */
    public void onExtraChunkReceived(final SPacketChunkData packet) {
        if (!ChunkFixConfig.instance.isLoadExtraChunks()) return;
        if (!insideRenderDistance(packet.getChunkX(), packet.getChunkZ())) {
            return;
        }
        if (null == mc.getConnection()) {
            log(WARN, "Not connected to game server, ignoring chunk for %d,%d", packet.getChunkX(), packet.getChunkZ());
            return;
        }
        try {
            Chunk loadedChunk = mc.world.getChunkProvider().getLoadedChunk(packet.getChunkX(), packet.getChunkZ());
            if (loadedChunk != null) {
                return; // already loaded, maybe by server, so ignore extra chunk
            }
            // load into game by pretending this comes from the game server
            mc.addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    mc.getConnection().handleChunkData(packet);
                }
            });
        } catch (NullPointerException ignored) {
        }
    }

    /**
     * track this chunk so we can unload it manually later
     */
    public void onChunkKeptFromUnloading(int cx, int cz) {
        // TODO remember to unload this chunk
    }

    /**
     * Periodically check for chunks to load/unload.
     */
    public void onTick() {
        if (!ChunkFixConfig.instance.isEnabled()) return;
        if (mc.player == null || mc.world == null) return;

        int skipClose = 4; // TODO make this configurable: server render distance + 1
        // no game chunks received for a second: request close-by chunks from chunk server
        if (lastTimeReceivedGameChunk < System.currentTimeMillis() - 1000) {
            skipClose = 0;
        }
        if (lastTimeRequestedExtraChunks < System.currentTimeMillis() - 1000
                && null != mc.getConnection()) {
            // don't send chunk requests when not connected
            // it will make us try inserting chunks into a nonexistent connection
            Collection<ChunkPos> extraChunks = PlayerChunkTracker.sortByPlayerDistance(PlayerChunkTracker.getUnloadedChunksInRenderDistance(skipClose));
            if (!extraChunks.isEmpty()) {
                connection.sendChunksRequest(extraChunks);
                lastTimeRequestedExtraChunks = System.currentTimeMillis();
            }
        }

        // XXX periodically unload chunks outside render distance
    }

    public static boolean insideRenderDistance(int cx, int cz) {
        if (mc.player == null) {
            logger.error("No player instance to check render distance from");
            return false;
        }
        int rd = mc.gameSettings.renderDistanceChunks;
        int cdx = mc.player.chunkCoordX - cx;
        int cdz = mc.player.chunkCoordZ - cz;
        return -rd <= cdx && cdx <= rd && -rd <= cdz && cdz <= rd;
    }
}
