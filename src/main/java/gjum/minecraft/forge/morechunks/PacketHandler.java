package gjum.minecraft.forge.morechunks;

import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.network.play.server.SPacketUnloadChunk;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * Hook into Minecraft's packet pipeline
 * to filter out unload packets to keep chunks rendered
 * and forward received chunks to the chunk server (done in {@link MoreChunks}).
 */
@ChannelHandler.Sharable
public class PacketHandler extends SimpleChannelInboundHandler<Packet<?>> implements ChannelOutboundHandler {

    static final String NAME = MoreChunksMod.MOD_ID + ":packet_handler";

    private final IMoreChunks moreChunks;

    public PacketHandler(IMoreChunks moreChunks) {
        this.moreChunks = moreChunks;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
        if (packet instanceof SPacketUnloadChunk) {
            return; // ignore packet, we manually unload our chunks
        }
        if (packet instanceof SPacketChunkData) {
            moreChunks.onReceiveGameChunk(convertChunk((SPacketChunkData) packet));
        }
        ctx.fireChannelRead(packet);
    }

    static Chunk convertChunk(SPacketChunkData chunkPacket) throws IOException {
        UnpooledByteBufAllocator alloc = new UnpooledByteBufAllocator(false);
        PacketBuffer pb = new PacketBuffer(alloc.buffer());

        chunkPacket.writePacketData(pb);
        pb.readLong(); // skip chunk coords
        byte[] chunkData = pb.readByteArray();

        pb.release();

        Pos2 pos = new Pos2(chunkPacket.getChunkX(), chunkPacket.getChunkZ());
        return new Chunk(pos, chunkData);
    }

    static SPacketChunkData convertChunk(Chunk chunk) throws IOException {
        UnpooledByteBufAllocator alloc = new UnpooledByteBufAllocator(false);
        PacketBuffer pb = new PacketBuffer(alloc.buffer());

        pb.writeLong(chunk.pos.asLong());
        pb.writeBytes(chunk.data);

        SPacketChunkData chunkPacket = new SPacketChunkData();
        chunkPacket.readPacketData(pb);

        pb.release();

        return chunkPacket;
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
