package com.github.rmheuer.voxel.level;

import com.github.rmheuer.voxel.block.Blocks;

import java.util.Arrays;

public final class MapSection {
    public static final int SIZE = 16;
    public static final int SIZE_SQUARED = SIZE * SIZE;
    public static final int SIZE_CUBED = SIZE * SIZE * SIZE;

    private final byte[] blocks;
    private short nonAirCount;

    public MapSection() {
        blocks = new byte[SIZE_CUBED];
        Arrays.fill(blocks, Blocks.ID_AIR);

        nonAirCount = 0;
    }

    private int blockIndex(int x, int y, int z) {
        return x + z * SIZE + y * SIZE_SQUARED;
    }

    public byte getBlockId(int x, int y, int z) {
        return blocks[blockIndex(x, y, z)];
    }

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

    public boolean isEmpty() {
        return nonAirCount == 0;
    }
}
