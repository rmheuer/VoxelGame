package com.github.rmheuer.voxel.render;

import com.github.rmheuer.voxel.block.Block;
import com.github.rmheuer.voxel.level.BlockMap;
import com.github.rmheuer.voxel.level.Blocks;
import com.github.rmheuer.voxel.level.LightMap;
import com.github.rmheuer.voxel.level.MapSection;

public final class SectionContext {
    private final BlockMap blockMap;
    private final MapSection section;

    private final LightMap lightMap;
    private final int originX, originY, originZ;

    public SectionContext(BlockMap blockMap, LightMap lightMap, int x, int y, int z) {
        this.blockMap = blockMap;
        section = blockMap.getSection(x, y, z);

        this.lightMap = lightMap;
        originX = x * MapSection.SIZE;
        originY = y * MapSection.SIZE;
        originZ = z * MapSection.SIZE;
    }

    public boolean isEmpty() {
        return section.isEmpty();
    }

    // XYZ must be in bounds of the section
    public Block getLocalBlock(int x, int y, int z) {
        return Blocks.getBlock(section.getBlockId(x, y, z));
    }

    // Returns null if outside world
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

    // XYZ must be in bounds for the world
    public boolean isLit(int x, int y, int z) {
        return lightMap.isLit(originX + x, originY + y, originZ + z);
    }
}
