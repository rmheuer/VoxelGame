package com.github.rmheuer.voxel.client.render;

import com.github.rmheuer.voxel.block.Block;
import com.github.rmheuer.voxel.level.BlockMap;
import com.github.rmheuer.voxel.block.Blocks;
import com.github.rmheuer.voxel.level.LightMap;
import com.github.rmheuer.voxel.level.MapSection;

/**
 * Provides access to the context surrounding a block when meshing.
 */
public final class SectionContext {
    private final BlockMap blockMap;
    private final MapSection section;

    private final LightMap lightMap;
    private final int originX, originY, originZ;

    /**
     * @param blockMap BlockMap the block is in
     * @param lightMap LightMap the block is in
     * @param x X coordinate of block
     * @param y Y coordinate of block
     * @param z Z coordinate of block
     */
    public SectionContext(BlockMap blockMap, LightMap lightMap, int x, int y, int z) {
        this.blockMap = blockMap;
        section = blockMap.getSection(x, y, z);

        this.lightMap = lightMap;
        originX = x * MapSection.SIZE;
        originY = y * MapSection.SIZE;
        originZ = z * MapSection.SIZE;
    }

    /**
     * Gets whether the section the block is in is empty
     */
    public boolean isEmpty() {
        return section.isEmpty();
    }

    /**
     * Gets a block within the current section. The position must be within
     * bounds of the section.
     *
     * @param x X coordinate of block
     * @param y Y coordinate of block
     * @param z Z coordinate of block
     * @return block at that position
     */
    public Block getLocalBlock(int x, int y, int z) {
        return Blocks.getBlock(section.getBlockId(x, y, z));
    }

    /**
     * Gets a block that could potentially be outside the section.
     *
     * @param x X coordinate of block within the section
     * @param y Y coordinate of block within the section
     * @param z Z coordinate of block within the section
     * @return block at that position, or null if outside the level
     */
    public Block getSurroundingBlock(int x, int y, int z) {
        int blockX = x + originX;
        int blockY = y + originY;
        int blockZ = z + originZ;

        int sectionX = Math.floorDiv(blockX, MapSection.SIZE);
        int sectionY = Math.floorDiv(blockY, MapSection.SIZE);
        int sectionZ = Math.floorDiv(blockZ, MapSection.SIZE);

        if (sectionX < 0 || sectionX >= blockMap.getSectionsX()
                || sectionY < 0 || sectionY >= blockMap.getSectionsY()
                || sectionZ < 0 || sectionZ >= blockMap.getSectionsZ()) {
            return null;
        }

        return Blocks.getBlock(blockMap.getSection(sectionX, sectionY, sectionZ).getBlockId(
                Math.floorMod(blockX, MapSection.SIZE),
                Math.floorMod(blockY, MapSection.SIZE),
                Math.floorMod(blockZ, MapSection.SIZE)
        ));
    }

    /**
     * Gets whether the block at a specified position is lit. The position must
     * be in bounds for the level.
     *
     * @param x X coordinate of block within the section
     * @param y Y coordinate of block within the section
     * @param z Z coordinate of block within the section
     * @return whether the block is lit by sky light
     */
    public boolean isLit(int x, int y, int z) {
        return lightMap.isLit(originX + x, originY + y, originZ + z);
    }
}
