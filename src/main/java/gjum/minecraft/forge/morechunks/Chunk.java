package gjum.minecraft.forge.morechunks;

public class Chunk {
    public final Pos2 pos;

    public final byte[] data;

    @Override
    public String toString() {
        return String.format("Chunk{%s}", pos);
    }

    public Chunk(Pos2 pos, byte[] data) {
        this.pos = pos;
        this.data = data;
    }
}
