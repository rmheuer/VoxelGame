package com.github.rmheuer.voxel.level;

import com.github.rmheuer.voxel.block.Blocks;

import java.util.Arrays;

/**
 * Stores the heights of sky light in the level
 */
public final class LightMap {
    /** Information about how a light column changed */
    public static final class Change {
        public final int prevHeight;
        public final int newHeight;

        public Change(int prevHeight, int newHeight) {
            this.prevHeight = prevHeight;
            this.newHeight = newHeight;
        }
    }

    private final int blocksX, blocksZ;
    private final int[] lightHeights;

    /**
     * Creates a new fully lit map.
     *
     * @param blocksX size of the map on the X axis
     * @param blocksZ size of the map on the Z axis
     */
    public LightMap(int blocksX, int blocksZ) {
        this.blocksX = blocksX;
        this.blocksZ = blocksZ;

        lightHeights = new int[blocksX * blocksZ];
        Arrays.fill(lightHeights, 0);
    }

    // Gets the index of a block column within the lightHeights array
    private int index(int blockX, int blockZ) {
        return blockX + blockZ * blocksX;
    }

    /**
     * Gets the Y coordinate of the highest lit block in a column.
     *
     * @param blockX x coordinate of the column
     * @param blockZ z coordinate of the column
     */
    public int getLightHeight(int blockX, int blockZ) {
        return lightHeights[index(blockX, blockZ)];
    }

    /**
     * Gets whether the block at the specified position is lit.
     *
     * @param blockX x coordinate of block
     * @param blockY y coordinate of block
     * @param blockZ z coordinate of block
     * @return whether the block is lit by sky light
     */
    public boolean isLit(int blockX, int blockY, int blockZ) {
        return getLightHeight(blockX, blockZ) <= blockY;
    }

    // Search downwards from startHeight to find the highest surface in a column
    private int findTopSurface(BlockMap map, int blockX, int blockZ, int startHeight) {
        int height;
        for (height = startHeight; height >= 0; height--) {
            if (Blocks.getBlock(map.getBlockId(blockX, height, blockZ)).isLightBlocking()) {
                break;
            }
        }
        return height;
    }

    /**
     * Recalculates the light heights for the entire level.
     *
     * @param map block map to calculate within
     */
    public void recalculateAll(BlockMap map) {
        int blocksY = map.getBlocksY();
        for (int z = 0; z < blocksZ; z++) {
            for (int x = 0; x < blocksX; x++) {
                int height = findTopSurface(map, x, z, blocksY - 1);
                lightHeights[index(x, z)] = height;
            }
        }
    }

    /**
     * Update the light based on a block change.
     *
     * @param map block map to calculate within
     * @param blockX x coordinate of block
     * @param blockY y coordinate of block
     * @param blockZ z coordinate of block
     * @param prevId ID of the block that was there previously
     * @param newId ID of the block that is there now
     * @return information about how the light in the column changed, null if
     *         the light column did not change
     */
    public Change blockChanged(BlockMap map, int blockX, int blockY, int blockZ, byte prevId, byte newId) {
        int index = index(blockX, blockZ);
        int currentHeight = lightHeights[index];
        if (blockY < currentHeight)
            return null;

        boolean prevOpaque = Blocks.getBlock(prevId).isLightBlocking();
        boolean newOpaque = Blocks.getBlock(newId).isLightBlocking();
        if (prevOpaque == newOpaque)
            return null;

        int newHeight;
        if (newOpaque) {
            // Light stops at this block now
            newHeight = blockY;
        } else {
            // Light travels down to next opaque block below
            newHeight = findTopSurface(map, blockX, blockZ, blockY - 1);
        }
        lightHeights[index] = newHeight;

        return new Change(currentHeight, newHeight);
    }

    /**
     * Gets whether a column position is within the bounds of the map.
     *
     * @param blockX x coordinate of column
     * @param blockZ z coordinate of column
     * @return whether the column is in bounds
     */
    public boolean isInBounds(int blockX, int blockZ) {
        return blockX >= 0 && blockX < blocksX
                && blockZ >= 0 && blockZ < blocksZ;
    }
}
