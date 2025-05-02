package com.github.rmheuer.voxel.level;

import com.github.rmheuer.azalea.math.AABB;
import com.github.rmheuer.voxel.block.Block;
import com.github.rmheuer.voxel.block.Blocks;
import com.github.rmheuer.voxel.block.Liquid;

import java.util.ArrayList;
import java.util.List;

public final class BlockMap {
    private final int sectionsX, sectionsY, sectionsZ;
    private final int blocksX, blocksY, blocksZ;
    private final MapSection[] sections;

    public BlockMap(int sectionsX, int sectionsY, int sectionsZ) {
        this.sectionsX = sectionsX;
        this.sectionsY = sectionsY;
        this.sectionsZ = sectionsZ;
        blocksX = sectionsX * MapSection.SIZE;
        blocksY = sectionsY * MapSection.SIZE;
        blocksZ = sectionsZ * MapSection.SIZE;

        int sectionCount = sectionsX * sectionsY * sectionsZ;

        sections = new MapSection[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            sections[i] = new MapSection();
        }
    }

    private int sectionIndex(int sectionX, int sectionY, int sectionZ) {
        return sectionX + sectionZ * sectionsX + sectionY * sectionsX * sectionsZ;
    }

    public MapSection getSection(int sectionX, int sectionY, int sectionZ) {
        return sections[sectionIndex(sectionX, sectionY, sectionZ)];
    }

    public MapSection getSectionContainingBlock(int blockX, int blockY, int blockZ) {
        int sectionX = blockX / MapSection.SIZE;
        int sectionY = blockY / MapSection.SIZE;
        int sectionZ = blockZ / MapSection.SIZE;

        return getSection(sectionX, sectionY, sectionZ);
    }

    public byte getBlockId(int blockX, int blockY, int blockZ) {
        MapSection section = getSectionContainingBlock(blockX, blockY, blockZ);
        int relX = blockX % MapSection.SIZE;
        int relY = blockY % MapSection.SIZE;
        int relZ = blockZ % MapSection.SIZE;

        return section.getBlockId(relX, relY, relZ);
    }

    public byte setBlockId(int blockX, int blockY, int blockZ, byte newBlockId) {
        MapSection section = getSectionContainingBlock(blockX, blockY, blockZ);
        int relX = blockX % MapSection.SIZE;
        int relY = blockY % MapSection.SIZE;
        int relZ = blockZ % MapSection.SIZE;

        return section.setBlockId(relX, relY, relZ, newBlockId);
    }

    public boolean isBlockInBounds(int blockX, int blockY, int blockZ) {
        return blockX >= 0 && blockX < blocksX
                && blockY >= 0 && blockY < blocksY
                && blockZ >= 0 && blockZ < blocksZ;
    }

    public List<AABB> getCollidersWithin(AABB region) {
        List<AABB> colliders = new ArrayList<>();

        int minX = (int) Math.floor(region.minX);
        int minZ = (int) Math.floor(region.minZ);
        int maxX = (int) Math.ceil(region.maxX);
        int maxZ = (int) Math.ceil(region.maxZ);

        int minY = (int) Math.max(Math.floor(region.minY), 0);
        int maxY = (int) Math.min(Math.ceil(region.maxY), blocksY);

        for (int y = minY; y < maxY; y++) {
            for (int z = minZ; z < maxZ; z++) {
                boolean zInBounds = z >= 0 && z < blocksZ;
                for (int x = minX; x < maxX; x++) {
                    boolean xInBounds = x >= 0 && x < blocksX;

                    if (xInBounds && zInBounds) {
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

    public boolean containsLiquid(AABB region, Liquid liquid) {
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
                    if (block.getLiquid() == liquid)
                        return true;
                }
            }
        }

        return false;
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
