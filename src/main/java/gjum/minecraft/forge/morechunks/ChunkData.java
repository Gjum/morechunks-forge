package gjum.minecraft.forge.morechunks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import static net.minecraft.world.chunk.Chunk.NULL_BLOCK_STORAGE;

public class ChunkData {
    public final int chunkX;
    public final int chunkZ;
    public final ExtendedBlockStorage[] sections;
    public final byte[] biomes = new byte[256];
    public final boolean isOverworld = true;

    public int[] heightMap;

    ChunkData(PacketBuffer packetBuffer) {
        chunkX = packetBuffer.readInt();
        chunkZ = packetBuffer.readInt();

        boolean packetHasAllSections = packetBuffer.readBoolean();

        int sectionsBitMask = packetBuffer.readVarInt();

        int sectionsByteCount = packetBuffer.readVarInt();
        if (sectionsByteCount > 0x200000) {
            throw new RuntimeException("Chunk Packet trying to allocate too much memory on read.");
        }

        sections = new ExtendedBlockStorage[16];

        for (int sectionNr = 0; sectionNr < sections.length; ++sectionNr) {
            ExtendedBlockStorage section = sections[sectionNr];
            if ((sectionsBitMask & 1 << sectionNr) == 0) {
                if (packetHasAllSections && section != NULL_BLOCK_STORAGE) {
                    sections[sectionNr] = NULL_BLOCK_STORAGE;
                }
            } else {
                if (section == NULL_BLOCK_STORAGE) {
                    section = new ExtendedBlockStorage(sectionNr << 4, packetHasAllSections);
                    sections[sectionNr] = section;
                }

                section.getData().read(packetBuffer);
                packetBuffer.readBytes(section.getBlocklightArray().getData());

                if (isOverworld) {
                    packetBuffer.readBytes(section.getSkylightArray().getData());
                }
            }
        }

        packetBuffer.readBytes(biomes);

        // ignore remaining data (tile entities)
    }

    /**
     * Finds the highest opaque block in each block column.
     *
     * @return array of the y-coordinates of the highest opaque blocks
     */
    public int[] calculateHeightMap() {
        heightMap = new int[256];

        for (int sectionNr = sections.length - 1; sectionNr >= 0; --sectionNr) {
            final ExtendedBlockStorage section = sections[sectionNr];

            if (section == NULL_BLOCK_STORAGE) continue;

            for (int xInSection = 0; xInSection < 16; ++xInSection) {
                for (int zInSection = 0; zInSection < 16; ++zInSection) {
                    final int blockColumnIndex = (zInSection << 4) + xInSection;

                    if (heightMap[blockColumnIndex] != 0) continue; // next block column

                    for (int yInSection = 15; yInSection >= 0; --yInSection) {
                        IBlockState block = section.get(xInSection, yInSection, zInSection);

                        if (block.isOpaqueCube()) {
                            heightMap[blockColumnIndex] = yInSection + section.getYLocation();
                            break; // block column loop
                        }
                    }
                }
            }
        }

        return heightMap;
    }

    public void replaceBottomSections(int topReplacedSection) {
        for (int sectionNr = 0; sectionNr <= topReplacedSection; sectionNr++) {
            sections[sectionNr] = NULL_BLOCK_STORAGE;
        }
    }

    public void serialize(PacketBuffer packetBuffer) {
        int sectionsBitMask = 0;
        int sectionsByteCount = 256; // biomes plus sections
        for (int sectionNr = 0; sectionNr < sections.length; ++sectionNr) {
            ExtendedBlockStorage extendedblockstorage = sections[sectionNr];

            if (extendedblockstorage != NULL_BLOCK_STORAGE) {
                sectionsBitMask |= 1 << sectionNr;
                sectionsByteCount += extendedblockstorage.getData().getSerializedSize();
                sectionsByteCount += extendedblockstorage.getBlocklightArray().getData().length;
                if (isOverworld) {
                    sectionsByteCount += extendedblockstorage.getSkylightArray().getData().length;
                }
            }
        }

        packetBuffer.writeInt(chunkX);
        packetBuffer.writeInt(chunkZ);
        packetBuffer.writeBoolean(biomes != null);
        packetBuffer.writeVarInt(sectionsBitMask);
        packetBuffer.writeVarInt(sectionsByteCount);

        for (int sectionNr = 0; sectionNr < sections.length; ++sectionNr) {
            ExtendedBlockStorage extendedblockstorage = sections[sectionNr];

            if (extendedblockstorage != NULL_BLOCK_STORAGE && (sectionsBitMask & 1 << sectionNr) != 0) {
                extendedblockstorage.getData().write(packetBuffer);
                packetBuffer.writeBytes(extendedblockstorage.getBlocklightArray().getData());
                if (isOverworld) {
                    packetBuffer.writeBytes(extendedblockstorage.getSkylightArray().getData());
                }
            }
        }

        packetBuffer.writeBytes(biomes);

        // we could just not write the tile entities here, because that's version specific
        // but we do it anyway, they'll just get ignored in other versions
        packetBuffer.writeVarInt(0);
    }
}
