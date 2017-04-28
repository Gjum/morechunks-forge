package gjum.minecraft.forge.chunkfix;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PlayerChunkTracker {
    private static Minecraft mc = Minecraft.getMinecraft();

    public static ChunkPos makeChunkPos(Long chunkPos) {
        return new ChunkPos((int) (long) chunkPos, (int) (chunkPos >> 32));
    }

    public static LongSet getLoadedChunkLongs() {
        ChunkProviderClient chunkProvider = mc.world.getChunkProvider();
        Long2ObjectMap<Chunk> chunkMapping = ObfuscationReflectionHelper.getPrivateValue(ChunkProviderClient.class, chunkProvider, "chunkMapping");
        return chunkMapping.keySet();
    }

    public static List<ChunkPos> getLoadedChunkList() {
        LongSet chunkLongs = getLoadedChunkLongs();
        ArrayList<ChunkPos> chunkPositions = new ArrayList<>();
        for (Long longPos : chunkLongs) {
            chunkPositions.add(makeChunkPos(longPos));
        }
        return chunkPositions;
    }

    public List<ChunkPos> getLoadedChunksOutsideRenderDistance() {
        ArrayList<ChunkPos> outside = new ArrayList<>();
        for (ChunkPos chunk : getLoadedChunkList()) {
            if (!ChunkFix.insideRenderDistance(chunk.chunkXPos, chunk.chunkZPos)) {
                outside.add(chunk);
            }
        }
        return outside;
    }

    /**
     * Build a list of all chunk positions inside the render distance
     * that are neither loaded nor inside the square around the player of radius skipClose.
     *
     * @param skipClose radius of the square around the player that will be ignored
     */
    public static List<ChunkPos> getUnloadedChunksInRenderDistance(int skipClose) {
        int px = mc.player.chunkCoordX;
        int pz = mc.player.chunkCoordZ;
        int rd = mc.gameSettings.renderDistanceChunks;
        LongSet loadedLongs = getLoadedChunkLongs();
        List<ChunkPos> unloaded = new ArrayList<>();
        for (int x = px - rd; x <= px + rd; x++) {
            for (int z = pz - rd; z <= pz + rd; z++) {
                if (px - skipClose < x && x < px + skipClose
                        && pz - skipClose < z && z < pz + skipClose) {
                    continue;
                }
                long longPos = ChunkPos.asLong(x, z);
                if (!loadedLongs.contains(longPos)) {
                    unloaded.add(new ChunkPos(x, z));
                }
            }
        }
        return unloaded;
    }

    /**
     * Sort by distance to the player, in-place, ascending.
     *
     * @param chunks chunks to sort
     * @return sorted chunks
     */
    public static List<ChunkPos> sortByPlayerDistance(List<ChunkPos> chunks) {
        Collections.sort(chunks, new Comparator<ChunkPos>() {
            @Override
            public int compare(ChunkPos a, ChunkPos b) {
                return Double.compare(a.getDistanceSq(mc.player), b.getDistanceSq(mc.player));
            }
        });
        return chunks;
    }
}
