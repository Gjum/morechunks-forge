package gjum.minecraft.forge.morechunks;

import gjum.minecraft.forge.chunkfix.ChunkFixMod;
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
import net.minecraft.util.LazyLoadBase;
import org.apache.logging.log4j.Level;

import java.net.SocketAddress;
import java.util.Collection;

import static net.minecraft.network.NetworkManager.CLIENT_EPOLL_EVENTLOOP;
import static net.minecraft.network.NetworkManager.CLIENT_NIO_EVENTLOOP;

public class ChunkServerConnection implements IChunkServerConnection {
    private final IConfig conf;
    private final IEnv env;
    private final IMoreChunks moreChunks;

    private Channel channel;

    private final byte RECV_CHUNK_DATA = 0;
    private final byte RECV_STATUS_MSG = 1;

    private final byte SEND_CHUNK_DATA = 0;
    private final byte SEND_STRING_MSG = 1;
    private final byte SEND_CHUNKS_REQUEST = 2;

    public ChunkServerConnection(IConfig conf, IEnv env, IMoreChunks moreChunks) {
        this.moreChunks = moreChunks;
        this.env = env;
        this.conf = conf;
    }

    @Override
    public synchronized void connect() {
        if (isConnected()) {
            env.log("ChunkServer", Level.WARN, "already connected");
            return;
        }

        env.log("ChunkServer", Level.INFO, "Connecting to %s:%d", conf.getHostname(), conf.getPort());

        Bootstrap bootstrap = new Bootstrap()
                .group(getLoopGroup().getValue())
                .channel(getChannelClass())
                .option(ChannelOption.SO_KEEPALIVE, true) // TODO what is this needed for?
                .handler(new ChannelInitializer<Channel>() {
                    protected void initChannel(Channel channel) {
                        channel.pipeline()
                                // every message is prepended with their length, so that we won't read partial messages or skip messages
                                .addLast("splitter", new LengthFieldBasedFrameDecoder(65535, 0, 4, 0, 4))
                                .addLast("prepender", new LengthFieldPrepender(4))
                                .addLast("packet_handler", new ReceiverHandler());
                    }
                });
        ChannelFuture connectedF = bootstrap.connect(conf.getHostname(), conf.getPort());

        channel = connectedF.channel();
        channel.closeFuture().addListener(closeF ->
                moreChunks.onChunkServerDisconnected(new DisconnectReason("ChunkServer: Connection closed")));

        connectedF.addListener(connectF -> {
            if (!connectF.isSuccess()) {
                channel.close();
                moreChunks.onChunkServerDisconnected(new DisconnectReason("ChunkServer: Connection failed"));
                return;
            }

            moreChunks.onChunkServerConnected();

            sendStringMsg("mod.version " + ChunkFixMod.VERSION);

            NetHandlerPlayClient mcConn = Minecraft.getMinecraft().getConnection();
            if (mcConn == null) {
                env.log("ChunkServer", Level.ERROR, "mcConn == null");
            } else {
                SocketAddress mcServerAddress = mcConn.getNetworkManager().channel().remoteAddress();
                sendStringMsg("game.address " + mcServerAddress);
            }
        });
    }

    @Override
    public void disconnect(DisconnectReason reason) {
        channel.disconnect();
        moreChunks.onChunkServerDisconnected(reason);
    }

    @Override
    public boolean isConnected() {
        return channel != null && channel.isOpen();
    }

    /**
     * Packet format:
     * - messageType (byte): 2
     * - numEntries (byte): how many chunks are requested
     * - chunkCoords (List<Long>): the coords of the requested chunks, combined into single Long values
     */
    @Override
    public void requestChunks(Collection<Pos2> chunksPos) {
        env.log("ChunkServer", Level.DEBUG, "Requesting %d chunks", chunksPos.size());
        ByteBuf buf = channel.alloc().buffer()
                .writeByte(SEND_CHUNKS_REQUEST);
        for (Pos2 pos : chunksPos) {
            buf.writeLong(pos.asLong());
        }
        channel.writeAndFlush(buf);
    }

    /**
     * Packet format:
     * - messageType (byte): 0
     * - timestamp (long): seconds since unix epoch, for selecting the latest chunk on the server side
     * - chunkPacket (bytes): the full chunk packet in Minecraft's internal format (1.10/1.11)
     */
    @Override
    public void sendChunk(Chunk chunk) {
        env.log("ChunkServer", Level.DEBUG, "sending chunk to server: %d,%d", chunk.pos.x, chunk.pos.z);
        channel.writeAndFlush(channel.alloc().buffer()
                .writeByte(SEND_CHUNK_DATA)
                .writeLong(env.currentTimeMillis())
                .writeLong(chunk.pos.asLong())
                .writeBytes(chunk.data)
        );
    }

    /**
     * Packet format:
     * - messageType (byte): 1
     * - message (bytes): encoded message string
     */
    public void sendStringMsg(String message) {
        env.log("ChunkServer", Level.DEBUG, "Sending msg " + message);
        channel.writeAndFlush(channel.alloc().buffer()
                .writeByte(SEND_STRING_MSG)
                .writeBytes(message.getBytes())
        );
    }

    private class ReceiverHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msgObj) {
            ByteBuf msg = (ByteBuf) msgObj;
            try {
                byte msgType = msg.readByte();
                switch (msgType) {

                    case RECV_CHUNK_DATA:
                        if (!msg.isReadable()) {
                            env.log("ChunkServer", Level.DEBUG, "empty chunk received");
                            break;
                        }

                        Pos2 pos = Pos2.fromLong(msg.readLong());
                        byte[] bytes = new byte[msg.readableBytes()];
                        msg.readBytes(bytes);

                        moreChunks.onReceiveExtraChunk(new Chunk(pos, bytes));
                        break;

                    case RECV_STATUS_MSG:
                        byte[] bb = new byte[msg.readableBytes()];
                        msg.readBytes(bb);
                        String statusMsg = new String(bb);

                        env.log("ChunkServer", Level.DEBUG, "StatusMsg received: " + statusMsg);
                        break;

                    default:
                        env.log("ChunkServer", Level.ERROR, "Unexpected message type %d 0x%02x", msgType, msgType);
                }
            } finally {
                msg.release();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
            moreChunks.onChunkServerDisconnected(new DisconnectReason("ChunkServer: Receiver Exception: " + cause.getMessage()));
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
