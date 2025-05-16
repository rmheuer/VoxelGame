package com.github.rmheuer.voxel.level;

import com.github.rmheuer.voxel.block.Blocks;

import java.util.Arrays;

/**
 * A cube section of a {@link BlockMap}.
 */
public final class MapSection {
    /** Size of the section along one axis. */
    public static final int SIZE = 16;

    public static final int SIZE_SQUARED = SIZE * SIZE;
    public static final int SIZE_CUBED = SIZE * SIZE * SIZE;

    private final byte[] blocks;
    private short nonAirCount;

    /**
     * Creates a new section filled with air.
     */
    public MapSection() {
        blocks = new byte[SIZE_CUBED];
        Arrays.fill(blocks, Blocks.ID_AIR);

        nonAirCount = 0;
    }

    public MapSection(byte[] blockData) {
        if (blockData.length != SIZE_CUBED)
            throw new IllegalArgumentException("Wrong block data size");
        blocks = blockData;

        nonAirCount = 0;
        for (byte b : blockData) {
            if (b != Blocks.ID_AIR) {
                nonAirCount++;
            }
        }
    }

    public MapSection(MapSection o) {
        blocks = o.blocks.clone();
        nonAirCount = o.nonAirCount;
    }

    public byte[] getBlockData() {
        return blocks;
    }

    // Gets the index of a block within the blocks array
    private int blockIndex(int x, int y, int z) {
        return x + z * SIZE + y * SIZE_SQUARED;
    }

    /**
     * Gets the ID of the block at the specified position.
     *
     * @param x x coordinate of block
     * @param y y coordinate of block
     * @param z z coordinate of block
     * @return ID of block at the position
     */
    public byte getBlockId(int x, int y, int z) {
        return blocks[blockIndex(x, y, z)];
    }

    /**
     * Sets the block at a specified position.
     *
     * @param x x coordinate of block
     * @param y y coordinate of block
     * @param z z coordinate of block
     * @param newBlockId ID of new block to set
     * @return ID of block that was there previously
     */
    public byte setBlockId(int x, int y, int z, byte newBlockId) {
        int index = blockIndex(x, y, z);

        byte oldBlockId = blocks[index];
        blocks[index] = newBlockId;

        if (newBlockId != oldBlockId) {
            if (newBlockId == Blocks.ID_AIR) {
                nonAirCount--;
            } else {
                nonAirCount++;
            }
        }

        return oldBlockId;
    }

    /**
     * Gets whether this section contains only air.
     *
     * @return whether the section is empty
     */
    public boolean isEmpty() {
        return nonAirCount == 0;
    }
}
