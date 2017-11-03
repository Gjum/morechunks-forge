package gjum.minecraft.forge.morechunks;

import io.netty.buffer.ByteBuf;

public class Chunk {
    public final Pos2 pos;

    public final ByteBuf data;

    @Override
    public String toString() {
        return String.format("Chunk{%s}", pos);
    }

    public Chunk(Pos2 pos, ByteBuf data) {
        this.pos = pos;
        this.data = data;
    }
}
