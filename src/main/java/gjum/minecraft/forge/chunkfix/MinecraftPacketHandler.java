package gjum.minecraft.forge.chunkfix;

import gjum.minecraft.forge.chunkfix.config.ChunkFixConfig;
import io.netty.channel.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.network.play.server.SPacketUnloadChunk;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.net.SocketAddress;

import static org.apache.logging.log4j.Level.ERROR;

/**
 * Hook into Minecraft's packet pipeline and listen for chunk data and unload packets
 * to filter out glitch chunk or unload packets to keep chunks rendered
 * and to forward the received chunks to the chunk server (done in {@link ChunkFix}).
 */
@ChannelHandler.Sharable
public class MinecraftPacketHandler extends SimpleChannelInboundHandler<Packet<?>> implements ChannelOutboundHandler {

    public static final String NAME = "chunkfix:packet_handler";
    private final ChunkFix chunkFix;

    public MinecraftPacketHandler(ChunkFix chunkFix) {
        this.chunkFix = chunkFix;
    }

    private boolean checkDiscardGlitchChunkPacket(Packet packet) {
        if (!(packet instanceof SPacketChunkData)) {
            return false;
        }
        if (!ChunkFixConfig.instance.isFilterGlitchChunks()) {
            return false;
        }
        SPacketChunkData p = (SPacketChunkData) packet;
        if (p.getChunkX() % 8 != 0 || p.getChunkZ() % 8 != 0) {
            return false;
        }
        World world = Minecraft.getMinecraft().world;
        if (world == null) {
            ChunkFix.log(ERROR, "Could not check chunk at %d,%d - no world present!", p.getChunkX(), p.getChunkZ());
            return false;
        }
        PacketBuffer pb = p.getReadBuffer();
        Chunk chunk = new Chunk(world, p.getChunkX(), p.getChunkZ());
        chunk.fillChunk(pb, p.getExtractedSize(), p.doChunkLoad());
        IBlockState blockState = chunk.getBlockState(0, 0, 0);
        boolean isBedrock = Blocks.BEDROCK.equals(blockState.getBlock());
        return !isBedrock;
    }

    private boolean checkDiscardUnloadPacket(Packet packet) {
        if (!(packet instanceof SPacketUnloadChunk)) {
            return false;
        }
        if (!ChunkFixConfig.instance.isKeepChunksLoaded()) {
            return false;
        }
        SPacketUnloadChunk p = (SPacketUnloadChunk) packet;
        if (ChunkFix.insideRenderDistance(p.getX(), p.getZ())) {
            chunkFix.onChunkKeptFromUnloading(p.getX(), p.getZ());
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
        if (checkDiscardGlitchChunkPacket(packet)) {
            return;
        }
        if (checkDiscardUnloadPacket(packet)) {
            return;
        }
        if (packet instanceof SPacketChunkData) {
            chunkFix.onRegularChunkReceived((SPacketChunkData) packet, System.currentTimeMillis() / 1000);
        }
        ctx.fireChannelRead(packet);
    }

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        ctx.bind(localAddress, promise);
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        ctx.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.disconnect(promise);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.close(promise);
    }

    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.deregister(promise);
    }

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ctx.write(msg, promise);
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
