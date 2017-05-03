package gjum.minecraft.forge.chunkfix;

import gjum.minecraft.forge.chunkfix.config.ChunkFixConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.util.LazyLoadBase;
import net.minecraft.util.math.ChunkPos;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;

import static java.util.concurrent.TimeUnit.SECONDS;
import static net.minecraft.network.NetworkManager.CLIENT_EPOLL_EVENTLOOP;
import static net.minecraft.network.NetworkManager.CLIENT_NIO_EVENTLOOP;
import static org.apache.logging.log4j.Level.ERROR;

/**
 * Maintains the connection to the chunk server.
 * Uses Minecraft's Netty event loop.
 */
public class Connection {
    private final ChunkFix chunkFix;
    private Channel channel;
    private int retryDelay = 1;

    public Connection(ChunkFix chunkFix) {
        this.chunkFix = chunkFix;
    }

    /**
     * Send a chunk packet to the chunk server.
     * <p>
     * Packet format:
     * - messageType (byte): 0
     * - timestamp (long): seconds since unix epoch, for selecting the latest chunk on the server side
     * - chunkPacket (bytes): the full chunk packet in Minecraft's internal format (1.10/1.11)
     */
    public ChannelFuture sendChunkPacket(SPacketChunkData chunkPacket, long timestamp) throws IOException {
        PacketBuffer pb = new PacketBuffer(channel.alloc().buffer());
        chunkPacket.writePacketData(pb);
        return channel.writeAndFlush(channel.alloc().buffer()
                .writeByte(0)
                .writeLong(timestamp)
                .writeBytes(pb.copy())
        );
    }

    /**
     * Send an arbitrary status message to the chunk server.
     * <p>
     * Packet format:
     * - messageType (byte): 1
     * - message (bytes): encoded message string
     */
    public ChannelFuture sendStringMsg(String message) {
        return channel.writeAndFlush(channel.alloc().buffer()
                .writeByte(1)
                .writeBytes(message.getBytes())
        );
    }

    /**
     * Request a list of chunks from the chunk server.
     * <p>
     * Packet format:
     * - messageType (byte): 2
     * - numEntries (byte): how many chunks are requested
     * - chunkCoords (List<Long>): the coords of the requested chunks, combined into single Long values
     */
    public ChannelFuture sendChunksRequest(Collection<ChunkPos> chunkPositions) {
        ByteBuf buf = channel.alloc().buffer()
                .writeByte(2);
        for (ChunkPos pos : chunkPositions) {
            buf.writeLong(ChunkPos.asLong(pos.chunkXPos, pos.chunkZPos));
        }
        return channel.writeAndFlush(buf);
    }

    public synchronized void connect() {
        if (channel != null && channel.isOpen()) {
            ChunkFix.log("already connected");
            return;
        }
        ChunkFixConfig conf = ChunkFixConfig.instance;
        ChunkFix.log("Connecting to %s:%d", conf.getHostname(), conf.getPort());
        ChannelFuture f = new Bootstrap()
                .group(getLoopGroup().getValue())
                .channel(getChannelClass())
                .option(ChannelOption.SO_KEEPALIVE, true) // TODO what is this needed for?
                .handler(new ChannelInitializer<Channel>() {
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline()
                                // every message is prepended with their length, so that we won't read partial messages or skip messages
                                .addLast("splitter", new LengthFieldBasedFrameDecoder(65535, 0, 4, 0, 4))
                                .addLast("prepender", new LengthFieldPrepender(4))
                                .addLast("packet_handler", new ReceiverHandler());
                    }
                })
                .connect(conf.getHostname(), conf.getPort())
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture f) throws Exception {
                        if (f.isSuccess()) {
                            retryDelay = 0;
                            ChunkFix.log("Connected: " + f.channel());

                            sendStringMsg("version " + ChunkFixMod.VERSION);

                            NetHandlerPlayClient mcConn = Minecraft.getMinecraft().getConnection();
                            if (mcConn != null) {
                                SocketAddress mcServerAddr = mcConn.getNetworkManager().channel().remoteAddress();
                                sendStringMsg("connected to " + mcServerAddr);
                            }
                        } else {
                            ChunkFix.log(ERROR, "connection failed: " + f);
                            f.channel().close();
                        }
                    }
                });
        channel = f.channel();
        channel.closeFuture().addListener(
                new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture f) throws Exception {
                        ChunkFix.log(ERROR, "connection closed: " + f);
                        reconnectAfterRetryDelay();
                    }
                }
        );
    }

    private void reconnectAfterRetryDelay() {
        ChunkFix.log("reconnecting");
        channel.eventLoop().schedule(new Runnable() {
            @Override
            public void run() {
                connect();
            }
        }, bumpRetryDelay(), SECONDS);
    }

    private int bumpRetryDelay() {
        retryDelay *= 2;
        if (retryDelay < 1) retryDelay = 1;
        if (retryDelay > 60) retryDelay = 60;
        return retryDelay;
    }

    private class ReceiverHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msgObj) {
            ByteBuf msg = (ByteBuf) msgObj;
            try {
                byte msgType = msg.readByte();
                switch (msgType) {

                    case 0: // chunk data
                        if (!msg.isReadable()) {
                            // empty chunk received
                            break;
                        }
                        SPacketChunkData chunkPacket = new SPacketChunkData();
                        chunkPacket.readPacketData(new PacketBuffer(msg));
                        chunkFix.onExtraChunkReceived(chunkPacket);
                        break;

                    case 1: // status message
                        byte[] bb = new byte[msg.readableBytes()];
                        msg.readBytes(bb);
                        String statusMsg = new String(bb);
                        ChunkFix.log("StatusMsg: " + statusMsg);
                        break;

                    default:
                        ChunkFix.log(ERROR, "Unexpected message type %d 0x%02x", msgType, msgType);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                msg.release();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
            reconnectAfterRetryDelay();
        }
    }

    private LazyLoadBase<? extends EventLoopGroup> getLoopGroup() {
        if (Epoll.isAvailable() && Minecraft.getMinecraft().gameSettings.useNativeTransport) {
            return CLIENT_EPOLL_EVENTLOOP;
        } else {
            return CLIENT_NIO_EVENTLOOP;
        }
    }

    private Class<? extends SocketChannel> getChannelClass() {
        if (Epoll.isAvailable() && Minecraft.getMinecraft().gameSettings.useNativeTransport) {
            return EpollSocketChannel.class;
        } else {
            return NioSocketChannel.class;
        }
    }
}
