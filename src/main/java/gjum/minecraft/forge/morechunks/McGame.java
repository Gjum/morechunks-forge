package gjum.minecraft.forge.morechunks;

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

    public McGame(IEnv env) {
        this.env = env;
    }

    @Override
    public List<Pos2> getLoadedChunks() {
        if (!mc.isCallingFromMinecraftThread()) {
            // try wrapping this getLoadedChunks() call in runOnMcThread() in your code
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
    public int getRenderDistance() {
        return mc.gameSettings.renderDistanceChunks;
    }

    @Override
    public boolean isIngame() {
        return mc.getConnection() != null;
    }

    @Override
    public void loadChunk(Chunk chunk) {
        // load into game by pretending this comes from the game server
        runOnMcThread(() -> {
            NetHandlerPlayClient conn = mc.getConnection();
            if (conn == null) {
                env.log(Level.ERROR, "mc.connection == null, ignoring extra chunk at %s", chunk.pos);
                return;
            }
            conn.handleChunkData(chunk.packet);
            env.log(Level.DEBUG, "Successfully loaded chunk at %s", chunk.pos);
        });
    }

    @Override
    public void runOnMcThread(Runnable runnable) {
        mc.addScheduledTask(runnable);
    }

    @Override
    public void unloadChunk(Pos2 pos) {
        runOnMcThread(() -> {
            NetHandlerPlayClient conn = mc.getConnection();
            if (conn == null) {
                env.log(Level.ERROR, "mc.connection == null, not unloading chunk at %s", pos);
                return;
            }
            conn.processChunkUnload(new SPacketUnloadChunk(pos.x, pos.z));
        });
    }

    private static LongSet getLoadedChunkLongs() {
        ChunkProviderClient chunkProvider = mc.world.getChunkProvider();
        Long2ObjectMap<Chunk> chunkMapping = ObfuscationReflectionHelper.getPrivateValue(ChunkProviderClient.class, chunkProvider, "chunkMapping");
        return chunkMapping.keySet();
    }
}
