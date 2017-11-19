package gjum.minecraft.forge.morechunks;

import io.netty.channel.ChannelPipeline;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketUnloadChunk;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.apache.logging.log4j.Level;

import java.util.ArrayList;
import java.util.List;

public class McGame implements IMcGame {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private IEnv env;
    private PacketHandler packetHandler;

    public McGame(IEnv env) {
        this.env = env;
    }

    @Override
    public String getCurrentServerIp() {
        if (null == mc.getCurrentServerData()) return null;
        return mc.getCurrentServerData().serverIP;
    }

    @Override
    public List<Pos2> getLoadedChunks() {
        if (!mc.isCallingFromMinecraftThread()) {
            // try wrapping this call in runOnMcThread() in your code
            throw new Error("Calling getLoadedChunks from non-mc-thread");
        }
        ArrayList<Pos2> chunkPositions = new ArrayList<>();
        for (Long longPos : getLoadedChunkLongs()) {
            chunkPositions.add(Pos2.fromLong(longPos));
        }
        return chunkPositions;
    }

    @Override
    public Pos2 getPlayerChunkPos() {
        return new Pos2(mc.player.chunkCoordX, mc.player.chunkCoordZ);
    }

    @Override
    public int getPlayerDimension() {
        return mc.player.dimension;
    }

    @Override
    public int getRenderDistance() {
        return mc.gameSettings.renderDistanceChunks;
    }

    @Override
    public void insertPacketHandler(IMoreChunks moreChunks) {
        NetHandlerPlayClient mcConnection = mc.getConnection();
        if (mcConnection == null) {
            env.log(Level.ERROR, "Could not inject packet handler into pipeline: mc.connection == null");
            return;
        }

        ChannelPipeline pipe = mcConnection.getNetworkManager().channel().pipeline();

        if (pipe.get(PacketHandler.NAME) != null) {
            env.log(Level.WARN, "game server connection pipeline already contains " + PacketHandler.NAME + ", removing and re-adding");
            pipe.remove(PacketHandler.NAME);
        }

        packetHandler = new PacketHandler(moreChunks);
        pipe.addBefore("fml:packet_handler", PacketHandler.NAME, packetHandler);

        env.log(Level.DEBUG, "Packet handler inserted");
    }

    @Override
    public boolean isIngame() {
        return mc.getConnection() != null;
    }

    @Override
    public void loadChunk(Chunk chunk) {
        if (!mc.isCallingFromMinecraftThread()) {
            // try wrapping this call in runOnMcThread() in your code
            throw new Error("Calling loadChunk from non-mc-thread");
        }

        // load into game by pretending this comes from the game server
        NetHandlerPlayClient conn = mc.getConnection();
        if (conn == null) {
            env.log(Level.ERROR, "mc.connection == null, ignoring extra chunk at %s", chunk.pos);
            return;
        }
        conn.handleChunkData(chunk.packet);
    }

    @Override
    public void runOnMcThread(Runnable runnable) {
        mc.addScheduledTask(runnable);
    }

    @Override
    public void unloadChunk(Pos2 pos) {
        if (!mc.isCallingFromMinecraftThread()) {
            // try wrapping this call in runOnMcThread() in your code
            throw new Error("Calling unloadChunk from non-mc-thread");
        }

        NetHandlerPlayClient conn = mc.getConnection();
        if (conn == null) {
            env.log(Level.ERROR, "mc.connection == null, not unloading chunk at %s", pos);
            return;
        }
        conn.processChunkUnload(new SPacketUnloadChunk(pos.x, pos.z));
    }

    @Override
    public boolean wasPacketHandlerAlreadyInserted() {
        NetHandlerPlayClient mcConnection = mc.getConnection();
        if (mcConnection == null) return false;

        ChannelPipeline pipe = mcConnection.getNetworkManager().channel().pipeline();
        return pipe.get(PacketHandler.NAME) != null;
    }

    private static LongSet getLoadedChunkLongs() {
        ChunkProviderClient chunkProvider = mc.world.getChunkProvider();
        Long2ObjectMap<Chunk> chunkMapping = ObfuscationReflectionHelper.getPrivateValue(ChunkProviderClient.class, chunkProvider, "chunkMapping");
        return chunkMapping.keySet();
    }
}
