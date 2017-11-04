package gjum.minecraft.forge.morechunks;

import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.apache.logging.log4j.Level;

import java.io.IOException;
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
        LongSet chunkLongs = getLoadedChunkLongs();
        ArrayList<Pos2> chunkPositions = new ArrayList<>();
        for (Long longPos : chunkLongs) {
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
        env.log(Level.DEBUG, "received extra chunk at %s", chunk.pos);

        SPacketChunkData packet;
        try {
            packet = convertChunk(chunk);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        env.log(Level.DEBUG, "converted extra chunk at %s", chunk.pos);

        // load into game by pretending this comes from the game server
        mc.addScheduledTask(() -> {
            NetHandlerPlayClient conn = mc.getConnection();
            if (conn == null) {
                env.log(Level.ERROR, "mc.connection == null, ignoring extra chunk at %s", chunk.pos);
                return;
            }
            conn.handleChunkData(packet);
            env.log(Level.DEBUG, "Successfully loaded chunk at %s", chunk.pos);
        });
    }

    @Override
    public void unloadChunk(Pos2 chunkPos) {
        env.log(Level.DEBUG, "unloading chunk at %s", chunkPos);
        // TODO implement
    }

    private static LongSet getLoadedChunkLongs() {
        ChunkProviderClient chunkProvider = mc.world.getChunkProvider();
        Long2ObjectMap<Chunk> chunkMapping = ObfuscationReflectionHelper.getPrivateValue(ChunkProviderClient.class, chunkProvider, "chunkMapping");
        return chunkMapping.keySet();
    }

    static Chunk convertChunk(SPacketChunkData chunkPacket) throws IOException {
        PacketBuffer pb = new PacketBuffer(new EmptyByteBuf(new UnpooledByteBufAllocator(true)));

        chunkPacket.writePacketData(pb);
        pb.readLong(); // skip chunk coords
        byte[] chunkData = pb.readByteArray();

        pb.release();

        Pos2 pos = new Pos2(chunkPacket.getChunkX(), chunkPacket.getChunkZ());
        return new Chunk(pos, chunkData);
    }

    static SPacketChunkData convertChunk(Chunk chunk) throws IOException {
        SPacketChunkData chunkPacket = new SPacketChunkData();
        PacketBuffer pb = new PacketBuffer(new EmptyByteBuf(new UnpooledByteBufAllocator(true)));

        pb.writeLong(chunk.pos.asLong());
        pb.writeBytes(chunk.data);

        chunkPacket.readPacketData(pb);

        pb.release();

        return chunkPacket;
    }
}
