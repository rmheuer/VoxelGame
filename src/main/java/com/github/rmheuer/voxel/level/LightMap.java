package com.github.rmheuer.voxel.level;

import java.util.Arrays;

public final class LightMap {
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

    public LightMap(int blocksX, int blocksZ) {
        this.blocksX = blocksX;
        this.blocksZ = blocksZ;

        lightHeights = new int[blocksX * blocksZ];
        Arrays.fill(lightHeights, 0);
    }

    private int index(int blockX, int blockZ) {
        return blockX + blockZ * blocksX;
    }

    public int getLightHeight(int blockX, int blockZ) {
        return lightHeights[index(blockX, blockZ)];
    }

    public boolean isLit(int blockX, int blockY, int blockZ) {
        return getLightHeight(blockX, blockZ) <= blockY;
    }

    private int findTopSurface(BlockMap map, int blockX, int blockZ, int startHeight) {
        int height;
        for (height = startHeight; height > 0; height--) {
            if (map.getBlockId(blockX, height - 1, blockZ) == Blocks.ID_SOLID) {
                break;
            }
        }
        return height;
    }

    public void recalculateAll(BlockMap map) {
        int blocksY = map.getBlocksY();
        for (int z = 0; z < blocksZ; z++) {
            for (int x = 0; x < blocksX; x++) {
                int height = findTopSurface(map, x, z, blocksY);
                lightHeights[index(x, z)] = height;
            }
        }
    }

    public Change blockChanged(BlockMap map, int blockX, int blockY, int blockZ, byte prevId, byte newId) {
        int index = index(blockX, blockZ);
        int currentHeight = lightHeights[index];
        if (blockY < currentHeight - 1)
            return null;

        boolean prevOpaque = prevId == Blocks.ID_SOLID;
        boolean newOpaque = newId == Blocks.ID_SOLID;
        if (prevOpaque == newOpaque)
            return null;

        int newHeight;
        if (newOpaque) {
            // Light stops above this block now
            newHeight = blockY + 1;
        } else {
            // Light travels down to next opaque block below
            newHeight = findTopSurface(map, blockX, blockZ, blockY);
        }
        lightHeights[index] = newHeight;

        return new Change(currentHeight, newHeight);
    }

    public boolean isInBounds(int blockX, int blockZ) {
        return blockX >= 0 && blockX < blocksX
                && blockZ >= 0 && blockZ < blocksZ;
    }
}
