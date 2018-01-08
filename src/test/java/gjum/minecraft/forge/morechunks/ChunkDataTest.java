package gjum.minecraft.forge.morechunks;

import io.netty.buffer.Unpooled;
import junit.framework.TestCase;
import net.minecraft.init.Bootstrap;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketChunkData;

import javax.xml.bind.DatatypeConverter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class ChunkDataTest extends TestCase {
    public void testChunkDataSerDe() throws Exception {
        final int chunkX = 6, chunkZ = 4;
        String path = String.format("/home/gjum/src/mc-misc/morechunks-client/chunk_dumps/chunk_%s_%s.pkt", chunkX, chunkZ);
        byte[] bytes_in = Files.readAllBytes(Paths.get(path));

        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeBytes(bytes_in);

        Bootstrap.register();

        final ChunkData chunkData = new ChunkData(buf);

        buf.clear();
        chunkData.serialize(buf);

        byte[] bytes_out = new byte[buf.readableBytes()];
        buf.readBytes(bytes_out);

        compareBytes(bytes_in, bytes_out);
    }

    public void testRemovingSectionsCompatibilityWithSPCD() throws Exception {
        final int chunkX = 6, chunkZ = 4;
        String path = String.format("/home/gjum/src/mc-misc/morechunks-client/chunk_dumps/chunk_%s_%s.pkt", chunkX, chunkZ);
        byte[] bytes_in = Files.readAllBytes(Paths.get(path));

        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeBytes(bytes_in);

        Bootstrap.register();

        final ChunkData chunkData = new ChunkData(buf);

        chunkData.replaceBottomSections(3);

        buf.clear();
        chunkData.serialize(buf);

        byte[] bytes_second = new byte[buf.readableBytes()];
        buf.copy().readBytes(bytes_second);

        final SPacketChunkData sPacketChunkData = new SPacketChunkData();
        sPacketChunkData.readPacketData(buf);

        buf.clear();
        sPacketChunkData.writePacketData(buf);

        byte[] bytes_out = new byte[buf.readableBytes()];
        buf.readBytes(bytes_out);

        compareBytes(bytes_second, bytes_out);
    }

    public void testSPCDSerDe() throws Exception {
        final int chunkX = 6, chunkZ = 4;
        String path = String.format("/home/gjum/src/mc-misc/morechunks-client/chunk_dumps/chunk_%s_%s.pkt", chunkX, chunkZ);
        byte[] bytes_in = Files.readAllBytes(Paths.get(path));

        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeBytes(bytes_in);

        final SPacketChunkData sPacketChunkData = new SPacketChunkData();
        sPacketChunkData.readPacketData(buf);

        buf.clear();
        sPacketChunkData.writePacketData(buf);

        byte[] bytes_out = new byte[buf.readableBytes()];
        buf.readBytes(bytes_out);

        compareBytes(bytes_in, bytes_out);
    }

    private void compareBytes(byte[] expected, byte[] actual) {
        List<String> failMsgs = new LinkedList<>();
        if (expected.length > actual.length) {
            final String cmp = compareByteStringsAt(expected, actual, actual.length, 16);
            failMsgs.add(String.format("too short: expected %d, was %d\n%s", expected.length, actual.length, cmp));
        }
        if (expected.length < actual.length) {
            final String cmp = compareByteStringsAt(expected, actual, expected.length, 16);
            failMsgs.add(String.format("too long: expected %d, was %d\n%s", expected.length, actual.length, cmp));
        }
        for (int i = 0; i < expected.length; i++) {
            byte e = expected[i];
            byte a = actual[i];
            if (e != a) {
                final String cmp = compareByteStringsAt(expected, actual, i, 16);
                failMsgs.add(String.format("bytes not equal: at %d\n%s", i, cmp));
                break;
            }
        }

        if (!failMsgs.isEmpty()) {
            fail(String.join("\n\n", failMsgs));
        }
    }

    private String compareByteStringsAt(byte[] expected, byte[] actual, int index, int context) {
        final int start = index - context < 0 ? 0 : index - context;
        final int eEnd = index + context > expected.length ? expected.length : index + context;
        final int aEnd = index + context > actual.length ? actual.length : index + context;
        final String eStr = DatatypeConverter.printHexBinary(expected).substring(start * 2, eEnd * 2).replaceAll("(..)", "$1 ");
        final String aStr = DatatypeConverter.printHexBinary(actual).substring(start * 2, aEnd * 2).replaceAll("(..)", "$1 ");
        final String marker = new String(new char[index - start]).replace("\0", "   ") + "<> #" + index;
        return String.format("%s\n%s\n%s", eStr, marker, aStr);
    }
}
