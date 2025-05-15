package com.github.rmheuer.voxel.level;

import com.github.rmheuer.azalea.math.AABB;
import com.github.rmheuer.voxel.block.Block;
import com.github.rmheuer.voxel.block.Blocks;
import com.github.rmheuer.voxel.block.Liquid;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Stores the grid of blocks in the level
 */
public final class BlockMap {
    private final int sectionsX, sectionsY, sectionsZ;
    private final int blocksX, blocksY, blocksZ;
    private final MapSection[] sections;

    /**
     * Creates a new map.
     *
     * @param sectionsX number of sections along the X axis
     * @param sectionsY number of sections along the Y axis
     * @param sectionsZ number of sections along the Z axis
     * @param blockData initial block data for the map
     */
    public BlockMap(int sectionsX, int sectionsY, int sectionsZ, byte[] blockData) {
        this.sectionsX = sectionsX;
        this.sectionsY = sectionsY;
        this.sectionsZ = sectionsZ;
        blocksX = sectionsX * MapSection.SIZE;
        blocksY = sectionsY * MapSection.SIZE;
        blocksZ = sectionsZ * MapSection.SIZE;

        int sectionCount = sectionsX * sectionsY * sectionsZ;

        sections = new MapSection[sectionCount];

        long before = System.nanoTime();
        for (int sectionY = 0; sectionY < sectionsY; sectionY++) {
            for (int sectionZ = 0; sectionZ < sectionsZ; sectionZ++) {
                for (int sectionX = 0; sectionX < sectionsX; sectionX++) {
                    int sectionBase = sectionX * 16 + sectionZ * 16 * blocksX + sectionY * 16 * blocksX * blocksZ;

                    byte[] sectionData = new byte[MapSection.SIZE_CUBED];

                    for (int blockY = 0; blockY < MapSection.SIZE; blockY++) {
                        for (int blockZ = 0; blockZ < MapSection.SIZE; blockZ++) {
                            int sectionOffset = blockZ * blocksX + blockY * blocksX * blocksZ;
                            int blockBase = blockZ * 16 + blockY * 256;

                            System.arraycopy(blockData, sectionBase + sectionOffset, sectionData, blockBase, 16);
                        }
                    }

                    sections[sectionIndex(sectionX, sectionY, sectionZ)] = new MapSection(sectionData);
                }
            }
        }
        long after = System.nanoTime();

        System.out.println("Level data unpacking took " + ((after - before) / 1_000_000.0) + " ms");
    }

    // Gets the index of the section within the sections array
    private int sectionIndex(int sectionX, int sectionY, int sectionZ) {
        return sectionX + sectionZ * sectionsX + sectionY * sectionsX * sectionsZ;
    }

    /**
     * Gets one section of the map at the specified position.
     *
     * @param sectionX x coordinate of the section
     * @param sectionY y coordinate of the section
     * @param sectionZ z coordinate of the section
     * @return section at those coordinates
     */
    public MapSection getSection(int sectionX, int sectionY, int sectionZ) {
        return sections[sectionIndex(sectionX, sectionY, sectionZ)];
    }

    /**
     * Gets the section containing the specified block position.
     *
     * @param blockX block position X to get section
     * @param blockY block position Y to get section
     * @param blockZ block position Z to get section
     * @return section containing the block position
     */
    public MapSection getSectionContainingBlock(int blockX, int blockY, int blockZ) {
        int sectionX = blockX / MapSection.SIZE;
        int sectionY = blockY / MapSection.SIZE;
        int sectionZ = blockZ / MapSection.SIZE;

        return getSection(sectionX, sectionY, sectionZ);
    }

    /**
     * Gets the ID of the block at the specified position.
     *
     * @param blockX x coordinate of block
     * @param blockY y coordinate of block
     * @param blockZ z coordinate of block
     * @return ID of block at the position
     */
    public byte getBlockId(int blockX, int blockY, int blockZ) {
        MapSection section = getSectionContainingBlock(blockX, blockY, blockZ);
        int relX = blockX % MapSection.SIZE;
        int relY = blockY % MapSection.SIZE;
        int relZ = blockZ % MapSection.SIZE;

        return section.getBlockId(relX, relY, relZ);
    }

    /**
     * Sets the block at a specified position.
     *
     * @param blockX x coordinate of block
     * @param blockY y coordinate of block
     * @param blockZ z coordinate of block
     * @param newBlockId ID of new block to set
     * @return ID of block that was there previously
     */
    public byte setBlockId(int blockX, int blockY, int blockZ, byte newBlockId) {
        MapSection section = getSectionContainingBlock(blockX, blockY, blockZ);
        int relX = blockX % MapSection.SIZE;
        int relY = blockY % MapSection.SIZE;
        int relZ = blockZ % MapSection.SIZE;

        return section.setBlockId(relX, relY, relZ, newBlockId);
    }

    /**
     * Gets whether a block position is within the bounds of the level.
     *
     * @param blockX x coordinate of block
     * @param blockY y coordinate of block
     * @param blockZ z coordinate of block
     * @return whether the position is in bounds
     */
    public boolean isBlockInBounds(int blockX, int blockY, int blockZ) {
        return blockX >= 0 && blockX < blocksX
                && blockY >= 0 && blockY < blocksY
                && blockZ >= 0 && blockZ < blocksZ;
    }

    /**
     * Gets the collision boxes of the blocks within a region. This will also
     * include collision boxes for the edge of the map if the region extends
     * out of bounds.
     *
     * @param region region to check
     * @return collision boxes within the region
     */
    public List<AABB> getCollidersWithin(AABB region) {
        List<AABB> colliders = new ArrayList<>();

        int minX = (int) Math.floor(region.minX);
        int minY = (int) Math.floor(region.minY);
        int minZ = (int) Math.floor(region.minZ);
        int maxX = (int) Math.ceil(region.maxX);
        int maxY = (int) Math.min(Math.ceil(region.maxY), blocksY);
        int maxZ = (int) Math.ceil(region.maxZ);

        for (int y = minY; y < maxY; y++) {
            boolean yInBounds = y >= 0;
            for (int z = minZ; z < maxZ; z++) {
                boolean zInBounds = z >= 0 && z < blocksZ;
                for (int x = minX; x < maxX; x++) {
                    boolean xInBounds = x >= 0 && x < blocksX;

                    if (xInBounds && yInBounds && zInBounds) {
                        Block block = Blocks.getBlock(getBlockId(x, y, z));

                        if (block.isSolid()) {
                            colliders.add(block.getBoundingBox().translate(x, y, z));
                        }
                    } else {
                        // Make horizontal border of world solid
                        colliders.add(new AABB(x, y, z, x + 1, y + 1, z + 1));
                    }
                }
            }
        }

        return colliders;
    }

    // Checks if a condition is true for any block within a region
    private boolean anyInRegionMatches(AABB region, Predicate<Block> condition) {
        int minX = (int) Math.max(Math.floor(region.minX), 0);
        int minY = (int) Math.max(Math.floor(region.minY), 0);
        int minZ = (int) Math.max(Math.floor(region.minZ), 0);
        int maxX = (int) Math.min(Math.ceil(region.maxX), blocksX);
        int maxY = (int) Math.min(Math.ceil(region.maxY), blocksY);
        int maxZ = (int) Math.min(Math.ceil(region.maxZ), blocksZ);

        for (int y = minY; y < maxY; y++) {
            for (int z = minZ; z < maxZ; z++) {
                for (int x = minX; x < maxX; x++) {
                    Block block = Blocks.getBlock(getBlockId(x, y, z));
                    if (condition.test(block))
                        return true;
                }
            }
        }

        return false;
    }

    /**
     * Gets whether a region contains the specified liquid.
     *
     * @param region region to check
     * @param liquid liquid to search for
     * @return whether the liquid is within the region
     */
    public boolean containsLiquid(AABB region, Liquid liquid) {
        return anyInRegionMatches(region, (block) -> block.getLiquid() == liquid);
    }

    /**
     * Gets whether a region contains any liquid.
     *
     * @param region region to check
     * @return whether any liquid is within the region
     */
    public boolean containsAnyLiquid(AABB region) {
        return anyInRegionMatches(region, (block) -> block.getLiquid() != null);
    }

    /**
     * Gets whether the region intersects no collision boxes and does not
     * contain any liquid.
     *
     * @param region region to check
     * @return whether the region is free
     */
    public boolean isFree(AABB region) {
        for (AABB collider : getCollidersWithin(region)) {
            if (collider.intersects(region))
                return false;
        }

        return !containsAnyLiquid(region);
    }

    public int getSectionsX() {
        return sectionsX;
    }

    public int getSectionsY() {
        return sectionsY;
    }

    public int getSectionsZ() {
        return sectionsZ;
    }

    public int getBlocksX() {
        return blocksX;
    }

    public int getBlocksY() {
        return blocksY;
    }

    public int getBlocksZ() {
        return blocksZ;
    }
}
